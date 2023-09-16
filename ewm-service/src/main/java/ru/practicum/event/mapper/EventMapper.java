package ru.practicum.event.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.expression.ParseException;
import org.springframework.stereotype.Component;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.State;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventMapper {

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Event toEvent(User user, NewEventDto newEventDto, Category category, Location location) {
        LocalDateTime timeNow = LocalDateTime.now();
        LocalDateTime eventDate;

        try {
            eventDate = LocalDateTime.parse(newEventDto.getEventDate(), formatter);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Не верный формат дат");
        }

        if (timeNow.plusHours(2).isAfter(eventDate)) {
            throw new ValidationException("eventDate не может быть раньше, чем через два часа от текущего момента");
        }

        Boolean paid = newEventDto.getPaid() != null ? newEventDto.getPaid() : false;
        Integer participantLimit = newEventDto.getParticipantLimit() != null ? newEventDto.getParticipantLimit() : 0;
        Boolean requestModeration = newEventDto
                .getRequestModeration() != null ? newEventDto.getRequestModeration() : true;

        return Event.builder()
                .title(newEventDto.getTitle())
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(eventDate)
                .location(location)
                .paid(paid)
                .participantLimit(participantLimit)
                .requestModeration(requestModeration)
                .category(category)
                .initiator(user)
                .createdOn(timeNow)
                .state(State.PENDING)
                .build();
    }

    public static EventFullDto toEventFullDto(Event event, Integer confirmedRequests, Integer views) {
        String publishedOn = event.getPublishedOn() != null ? event.getPublishedOn().format(formatter) : null;

        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(formatter))
                .location(event.getLocation())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .createdOn(event.getCreatedOn().format(formatter))
                .publishedOn(publishedOn)
                .confirmedRequests(confirmedRequests)
                .state(event.getState().toString())
                .views(views)
                .build();
    }

    public static EventShortDto toEventShortDto(Event event, Integer confirmedRequest, Integer views) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .eventDate(event.getEventDate().format(formatter))
                .paid(event.getPaid())
                .confirmedRequests(confirmedRequest)
                .views(views)
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .category(CategoryMapper.toCategoryDto(event.getCategory()))
                .build();
    }

    public static EventFullDto toEventFullDto(EventWithConfirmedRequest eventWithConfirmedRequest, Integer views) {
        return EventFullDto.builder()
                .id(eventWithConfirmedRequest.getId())
                .title(eventWithConfirmedRequest.getTitle())
                .annotation(eventWithConfirmedRequest.getAnnotation())
                .description(eventWithConfirmedRequest.getDescription())
                .eventDate(eventWithConfirmedRequest.getEventDate().format(formatter))
                .location(eventWithConfirmedRequest.getLocation())
                .paid(eventWithConfirmedRequest.getPaid())
                .participantLimit(eventWithConfirmedRequest.getParticipantLimit())
                .requestModeration(eventWithConfirmedRequest.getRequestModeration())
                .category(CategoryMapper.toCategoryDto(eventWithConfirmedRequest.getCategory()))
                .initiator(UserMapper.toUserShortDto(eventWithConfirmedRequest.getInitiator()))
                .createdOn(eventWithConfirmedRequest.getCreatedOn().format(formatter))
                .publishedOn(eventWithConfirmedRequest.getPublishedOn().format(formatter))
                .confirmedRequests(Math.toIntExact(eventWithConfirmedRequest.getConfirmedRequests()))
                .views(views)
                .build();
    }

    public static EventShortDto toEventShortDto(EventWithConfirmedRequest eventWithConfirmedRequest, Integer views) {
        return EventShortDto.builder()
                .id(eventWithConfirmedRequest.getId())
                .title(eventWithConfirmedRequest.getTitle())
                .annotation(eventWithConfirmedRequest.getAnnotation())
                .eventDate(eventWithConfirmedRequest.getEventDate().format(formatter))
                .paid(eventWithConfirmedRequest.getPaid())
                .confirmedRequests(Math.toIntExact(eventWithConfirmedRequest.getConfirmedRequests()))
                .views(views)
                .initiator(UserMapper.toUserShortDto(eventWithConfirmedRequest.getInitiator()))
                .category(CategoryMapper.toCategoryDto(eventWithConfirmedRequest.getCategory()))
                .build();
    }

    public static Event update(Event event, UpdateEventRequest request) {

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(LocalDateTime.parse(request.getEventDate(), formatter));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        return event;
    }
}
