package ru.practicum.event.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.expression.ParseException;
import org.springframework.stereotype.Component;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.State;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.exception.ConflictException;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;

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
            throw new ConflictException("eventDate не может быть раньше, чем через два часа от текущего момента");
        }

        return Event.builder()
                .title(newEventDto.getTitle())
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .eventDate(eventDate)
                .location(location)
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
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
}
