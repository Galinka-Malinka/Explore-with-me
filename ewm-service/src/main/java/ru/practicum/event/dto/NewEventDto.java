package ru.practicum.event.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Location;

import javax.validation.constraints.*;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {

    @NotNull
    @NotBlank
    @Size(min = 3, max = 120, message = "Размер заголовка не входит в допустимый диапазон от {min} до {max} символов")
    String title;

    @NotNull
    @NotBlank
    @Size(min = 20, max = 2000, message = "Размер аннотации не входит в допустимый диапазон от {min} до {max} символов")
    String annotation;

    @NotNull
    @NotBlank
    @Size(min = 20, max = 7000, message = "Размер описания не входит в допустимый диапазон от {min} до {max} символов")
    String description;

    @NotNull
    @NotBlank
    String eventDate;  //Дата и время на которые намечено событие в формате "yyyy-MM-dd HH:mm:ss"

    @NotNull
    Location location;

    Boolean paid;  //default: false

    @PositiveOrZero
    Integer participantLimit;  //default: 0

    Boolean requestModeration;  //default: true

    @NotNull
    Integer category;

}
