package ru.practicum.user.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDto {

    Integer id;

    @NotNull(message = "Имя пользователя не заданно")
    @NotBlank(message = "Имя пользователя не может состоять из пустой строки")
    private String name;

    @NotNull(message = "email пользователя не задан")
    @NotBlank(message = "email пользователя не может состоять из пустой строки")
    @Email(message = "Не верный формат email")
    private String email;
}
