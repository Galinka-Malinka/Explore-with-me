package ru.practicum.comment.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShortCommentDto {
    @NotNull
    @NotBlank
    String authorName;

    @NotNull(message = "Необходимо добавить содержимое комментария")
    @NotBlank(message = "Комментарий не может состоять из пустой строки")
    String text;

    @NotNull
    LocalDateTime created;
}
