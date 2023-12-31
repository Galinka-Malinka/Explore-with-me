package ru.practicum.comment.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FullCommentDto {

    Integer id;

    String eventTitle;

    String text;

    User author;

    LocalDateTime created;
}


