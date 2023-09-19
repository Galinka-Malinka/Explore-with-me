package ru.practicum.comment.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.comment.dto.FullCommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.event.model.Event;
import ru.practicum.user.model.User;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommentMapper {

    public static Comment toComment(@NotNull NewCommentDto newCommentDto, @NotNull User author, @NotNull Event event) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .event(event)
                .author(author)
                .created(LocalDateTime.now())
                .build();
    }

    public static FullCommentDto toFullCommentDto(@NotNull Comment comment) {
        return FullCommentDto.builder()
                .id(comment.getId())
                .eventTitle(comment.getEvent().getTitle())
                .text(comment.getText())
                .author(comment.getAuthor())
                .created(comment.getCreated())
                .build();
    }

    public static List<FullCommentDto> toFullCommentDtoList(List<Comment> comments) {
        List<FullCommentDto> fullCommentDtoList = new ArrayList<>();
        if (comments != null) {
            for (Comment comment : comments) {
                fullCommentDtoList.add(toFullCommentDto(comment));
            }
        }
        return fullCommentDtoList;
    }
}

