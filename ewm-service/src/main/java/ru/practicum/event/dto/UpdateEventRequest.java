package ru.practicum.event.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Location;

import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventRequest {

    String title;
    String annotation;
    String description;

    String eventDate;
    Location location;

    Boolean paid;

    Integer participantLimit;

    Integer category;

    Boolean requestModeration;

    String stateAction;

}

