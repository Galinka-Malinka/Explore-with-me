package ru.practicum.user.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDto {

    Integer id;

    @NotNull(message = "Имя пользователя не заданно")
    @NotBlank(message = "Имя пользователя не может состоять из пустой строки")
    @Size(min = 2, max = 250, message = "Имя пользователя не входит в допустимый диапазон от {min} до {max} символов")
    String name;

    @NotNull(message = "email пользователя не задан")
    @NotBlank(message = "email пользователя не может состоять из пустой строки")
    @Email(message = "Не верный формат email")
    @Size(min = 6, max = 254, message = "Email пользователя не входит в допустимый диапазон от {min} до {max} символов")
    String email;
}
