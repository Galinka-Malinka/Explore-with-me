package ru.practicum.comment.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewCommentDto {

    @JsonCreator
    public NewCommentDto(@JsonProperty("text") String text) {
        this.text = text;
    }

    @NotNull(message = "Необходимо добавить содержимое комментария")
    @NotBlank(message = "Комментарий не может состоять из пустой строки")
    String text;

}
