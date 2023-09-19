package ru.practicum.comment.service;

import ru.practicum.comment.dto.FullCommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.ShortCommentDto;

import java.util.List;

public interface CommentService {

    FullCommentDto create(Integer userId, Integer eventId, NewCommentDto newCommentDto);

    List<FullCommentDto> getComments(Integer[] events, Integer[] users, String text,
                                     String rangeStart, String rangeEnd, Integer from, Integer size);

    void delete(Integer[] ids, Integer[] events);

    List<ShortCommentDto> getCommentsByEvent(Integer eventId);

}

