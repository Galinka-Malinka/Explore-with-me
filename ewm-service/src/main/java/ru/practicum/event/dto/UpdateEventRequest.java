package ru.practicum.event.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Location;

import javax.validation.constraints.Size;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventRequest {

    @Size(min = 3, max = 120, message = "Размер заголовка не входит в диапазон от {min} до {max} символов")
    String title;

    @Size(min = 20, max = 2000, message = "Размер аннотации не выходит в диапазон от {min} до {max} символов")
    String annotation;

    @Size(min = 20, max = 7000, message = "Размер описания не входит в диапазон от {min} до {max} символов")
    String description;

    String eventDate;  //Новые дата и время на которые намечено событие. Дата и время указываются в формате "yyyy-MM-dd HH:mm:ss"

    Location location;

    Boolean paid;

    Integer participantLimit;

    Integer category;

    Boolean requestModeration;

    String stateAction;

}

