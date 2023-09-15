package ru.practicum.event.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Location;

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

