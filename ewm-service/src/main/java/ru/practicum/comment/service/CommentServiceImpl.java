package ru.practicum.comment.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comment.dto.FullCommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.ShortCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.model.QComment;
import ru.practicum.comment.storage.CommentStorage;
import ru.practicum.event.State;
import ru.practicum.event.model.Event;
import ru.practicum.event.storage.EventStorage;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.model.User;
import ru.practicum.user.storage.UserStorage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final UserStorage userStorage;

    private final EventStorage eventStorage;

    private final CommentStorage commentStorage;

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public FullCommentDto create(Integer userId, Integer eventId, NewCommentDto newCommentDto) {

        User user = userStorage.findById(userId).orElseThrow(() ->
                new NotFoundException("Пользователь с id " + userId + " не найден"));

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new ConflictException("Нельзя добавить комментарий не опубликованному событию с id " + eventId);
        }

        Comment comment = CommentMapper.toComment(newCommentDto, user, event);

        return CommentMapper.toFullCommentDto(commentStorage.save(comment));
    }

    @Override
    public List<FullCommentDto> getComments(Integer[] events, Integer[] users, String text,
                                            String rangeStart, String rangeEnd, Integer from, Integer size) {
        List<FullCommentDto> fullCommentDtoList = new ArrayList<>();

        if (rangeStart != null && rangeEnd != null
                && LocalDateTime.parse(rangeStart, formatter).isAfter(LocalDateTime.parse(rangeEnd, formatter))) {
            throw new ConflictException("Окончание временного диапазона не может быть раньше его начала");
        }

        int page = from / size;

        Pageable sortedAndPageable =
                PageRequest.of(page, size, Sort.by("id"));

        QComment comment = QComment.comment;

        List<BooleanExpression> parameters = new ArrayList<>();

        if (events != null) {
            parameters.add(comment.event.id.in(events));
        }
        if (users != null) {
            parameters.add(comment.author.id.in(users));
        }
        if (text != null) {
            parameters.add(comment.text.containsIgnoreCase(text));
        }
        if (rangeStart != null) {
            parameters.add(comment.created.after(LocalDateTime.parse(rangeStart, formatter)));
        }
        if (rangeEnd != null) {
            parameters.add(comment.created.before(LocalDateTime.parse(rangeEnd, formatter)));
        }

        List<Comment> comments;

        if (parameters.isEmpty()) {
            comments = commentStorage.findAll(sortedAndPageable).toList();
        } else {
            BooleanExpression readyParameters = parameters.stream().reduce(BooleanExpression::and).get();
            comments = commentStorage.findAll(readyParameters, sortedAndPageable).toList();
        }

        if (!comments.isEmpty()) {
            fullCommentDtoList = CommentMapper.toFullCommentDtoList(comments);
        }

        return fullCommentDtoList;
    }

    @Override
    @Transactional
    public void delete(Integer[] ids, Integer[] events) {
        if (ids == null && events == null) {
            commentStorage.deleteAll();
        } else if (ids != null && events != null) {
            commentStorage.deleteAllByIdIn(ids);
            commentStorage.deleteAllByEventIdIn(events);
        } else if (ids != null) {
            commentStorage.deleteAllByIdIn(ids);
        } else {
            commentStorage.deleteAllByEventIdIn(events);
        }
    }

    @Override
    public List<ShortCommentDto> getCommentsByEvent(Integer eventId) {

        if (!eventStorage.existsById(eventId)) {
            throw new NotFoundException("Событие с id " + eventId + " не найдено");
        }

        List<ShortCommentDto> shortCommentDtoList = new ArrayList<>();

        if (commentStorage.existsByEventId(eventId)) {
            shortCommentDtoList = commentStorage.findByEventId(eventId);
        }
        return shortCommentDtoList;
    }
}
