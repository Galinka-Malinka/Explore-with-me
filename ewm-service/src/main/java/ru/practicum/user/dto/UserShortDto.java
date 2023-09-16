package ru.practicum.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserShortDto {

    @NotNull(message = "id пользователя не заданно")
    Integer id;

    @NotNull(message = "Имя пользователя не заданно")
    @NotBlank(message = "Имя пользователя не может состоять из пустой строки")
    private String name;
}
