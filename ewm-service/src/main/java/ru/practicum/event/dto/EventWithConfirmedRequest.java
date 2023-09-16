package ru.practicum.event.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.practicum.category.model.Category;
import ru.practicum.event.State;
import ru.practicum.event.model.Location;
import ru.practicum.user.model.User;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventWithConfirmedRequest {
    Integer id;
    String title;
    String annotation;
    String description;
    LocalDateTime eventDate;
    Location location;
    Boolean paid;
    Integer participantLimit;
    Category category;
    Boolean requestModeration;
    User initiator;
    LocalDateTime createdOn;
    LocalDateTime publishedOn;
    State state;
    Long confirmedRequests;
}
