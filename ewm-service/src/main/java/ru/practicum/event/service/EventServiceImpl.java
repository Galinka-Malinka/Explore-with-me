package ru.practicum.event.service;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.category.model.Category;
import ru.practicum.category.storage.CategoryStorage;
import ru.practicum.client.EventEndpointHitClient;
import ru.practicum.client.EventViewStatsClient;
import ru.practicum.event.State;
import ru.practicum.event.dto.*;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.model.QEvent;
import ru.practicum.event.storage.EventStorage;
import ru.practicum.event.storage.LocationStorage;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationRequest.Status;
import ru.practicum.participationRequest.storage.ParticipationRequestStorage;
import ru.practicum.user.model.User;
import ru.practicum.user.storage.UserStorage;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {


    private final EventViewStatsClient eventViewStatsClient;

    private final EventEndpointHitClient eventEndpointHitClient;

    private final EventStorage eventStorage;

    private final UserStorage userStorage;

    private final CategoryStorage categoryStorage;

    private final LocationStorage locationStorage;

    private final ParticipationRequestStorage participationRequestStorage;


    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    @Override
    public EventFullDto create(Integer userId, NewEventDto newEventDto) {
        User user = userStorage.findById(userId).orElseThrow(() ->
                new NotFoundException("Пользователь с id " + userId + " не найден"));

        Category category = categoryStorage.findById(newEventDto.getCategory()).orElseThrow(() ->
                new NotFoundException("Категория с id " + newEventDto.getCategory() + " не найдена"));

        Location location = locationStorage.save(newEventDto.getLocation());

        Event event = eventStorage.save(EventMapper.toEvent(user, newEventDto, category, location));

        return EventMapper.toEventFullDto(event, 0, 0);
    }

    @Override
    public EventFullDto getByInitiatorById(Integer userId, Integer eventId) {
        checkUser(userId);

        if (eventStorage.existsByIdAndState(eventId, State.PUBLISHED)) {

            if (participationRequestStorage.existsByEventId(eventId)) {
                EventWithConfirmedRequest eventWithConfirmedRequest = participationRequestStorage
                        .getEvenWithConfirmedRequests(eventId, Status.CONFIRMED);

                Integer views = getViews(eventWithConfirmedRequest.getPublishedOn(), eventId);
                return EventMapper.toEventFullDto(eventWithConfirmedRequest, views);
            } else {
                Event event = eventStorage.findById(eventId)
                        .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

                Integer views = getViews(event.getPublishedOn(), eventId);
                return EventMapper.toEventFullDto(event, 0, views);
            }
        } else {
            Event event = eventStorage.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
            return EventMapper.toEventFullDto(event, null, null);
        }
    }

    @Override
    public List<EventShortDto> getByInitiator(Integer userId, Integer from, Integer size) {
        checkUser(userId);

        int page = from / size;

        Pageable sortedAndPageable =
                PageRequest.of(page, size, Sort.by("id"));

        List<Event> events = eventStorage.findAllByInitiatorId(userId, sortedAndPageable);

        List<EventShortDto> eventShortDtoList = new ArrayList<>();

        if (!events.isEmpty()) {
            for (Event event : events) {

                Integer confirmedRequests = null;
                Integer views = null;

                if (event.getState().equals(State.PUBLISHED)) {
                    List<Integer> confirmedRequestsAndViews = getConfirmedRequestAndViews(event);
                    confirmedRequests = confirmedRequestsAndViews.get(0);
                    views = confirmedRequestsAndViews.get(1);
                }

                eventShortDtoList.add(EventMapper.toEventShortDto(event, confirmedRequests, views));
            }
        }
        return eventShortDtoList;
    }

    @Transactional
    @Override
    public EventFullDto update(Integer userId, Integer eventId, UpdateEventRequest request) {
        checkUser(userId);

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (event.getInitiator().getId() != userId) {
            throw new NotFoundException("Пользователь с id " + userId +
                    " не является инициатором события с id " + eventId);
        }

        if (event.getState().equals(State.PUBLISHED)) {
            throw new ConflictException("Изменить можно только отмененные события" +
                    " или события в состоянии ожидания модерации");
        }

        LocalDateTime controlTime = LocalDateTime.now().plusHours(2);

        if (request.getEventDate() == null && event.getEventDate().isBefore(controlTime) ||
                request.getEventDate() != null && LocalDateTime.parse(request.getEventDate(), formatter)
                        .isBefore(controlTime)) {
            throw new ConflictException("Дата и время на которые намечено событие" +
                    " не может быть раньше, чем через два часа от текущего момента");
        }


        Event eventWithUpdatedParameters = updateEventByParameters(event, request);

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals(State.CANCEL_REVIEW.toString())) {
                eventWithUpdatedParameters.setState(State.CANCEL_REVIEW);
            } else {
                throw new ConflictException("Обновление состояния события пользователем на "
                        + request.getStateAction() + " не возможно");
            }
        }

        Event updatedEvent = eventStorage.save(eventWithUpdatedParameters);

        return EventMapper.toEventFullDto(updatedEvent, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getByAdmin(Integer[] users, String[] states, Integer[] categories,
                                         String rangeStart, String rangeEnd, Integer from, Integer size) {
        List<EventFullDto> eventFullDtoList = new ArrayList<>();

        int page = from / size;

        Pageable sortedAndPageable =
                PageRequest.of(page, size, Sort.by("id"));

        QEvent event = QEvent.event;

        BooleanBuilder where = new BooleanBuilder();

        if (users != null) {
            where.and(event.initiator.id.in(users));
        }
        if (states != null) {
            where.and(event.state.stringValue().in(states));
        }
        if (categories != null) {
            where.and(event.category.id.in(categories));
        }
        if (rangeStart != null) {
            where.and(event.eventDate.after(LocalDateTime.parse(rangeStart, formatter)));
        }
        if (rangeEnd != null) {
            where.and(event.eventDate.before(LocalDateTime.parse(rangeEnd, formatter)));
        }

        Iterable<Event> foundEvents = eventStorage.findAll(where, sortedAndPageable);


        for (Event foundEvent : foundEvents) {
            Integer confirmedRequests = null;
            Integer views = null;

            if (foundEvent.getState().equals(State.PUBLISHED)) {
                List<Integer> confirmedRequestsAndViews = getConfirmedRequestAndViews(foundEvent);
                confirmedRequests = confirmedRequestsAndViews.get(0);
                views = confirmedRequestsAndViews.get(1);
            }
            eventFullDtoList.add(EventMapper.toEventFullDto(foundEvent, confirmedRequests, views));
        }

        return eventFullDtoList;
    }

    @Transactional
    @Override
    public EventFullDto updateByAdmin(Integer eventId, UpdateEventRequest request) {

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        LocalDateTime timeNow = LocalDateTime.now();

        if (event.getEventDate() != null && !event.getEventDate().minusHours(1).isAfter(timeNow)) {
            throw new ConflictException("Дата начала изменяемого события должна быть" +
                    " не ранее чем за час от даты публикации");
        }

        if (request.getStateAction() != null && request.getStateAction().equals(State.PUBLISH_EVENT.toString()) &&
                !event.getState().equals(State.PENDING)) {
            throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации");
        }

        if (request.getStateAction() != null && request.getStateAction().equals(State.CANCELED_EVENT.toString()) &&
                event.getState().equals(State.PUBLISHED)) {
            throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано");
        }

        Event eventWithUpdatedParameters = updateEventByParameters(event, request);

        if (request.getStateAction() != null && request.getStateAction().equals(State.PUBLISH_EVENT.toString())) {
            eventWithUpdatedParameters.setState(State.PUBLISHED);
            eventWithUpdatedParameters.setPublishedOn(LocalDateTime.now());
        }

        if (request.getStateAction() != null && request.getStateAction().equals(State.CANCELED_EVENT.toString())) {
            eventWithUpdatedParameters.setState(State.CANCELED);
        }

        Event updatedEvent = eventStorage.save(eventWithUpdatedParameters);

        return EventMapper.toEventFullDto(updatedEvent, null, null);
    }

    @Override
    public List<EventShortDto> getPublicly(String text, Integer[] categories, Boolean paid,
                                           String rangeStart, String rangeEnd, Boolean onlyAvailable, String sort,
                                           Integer from, Integer size, HttpServletRequest request) {
        List<EventShortDto> result = new ArrayList<>();

        int page = from / size;

        Pageable sortedAndPageable =
                PageRequest.of(page, size, Sort.by("id"));

        QEvent event = QEvent.event;

        BooleanBuilder where = new BooleanBuilder();

        where.and(event.state.eq(State.PUBLISHED));

        if (text != null) {
            where.and(event.annotation.containsIgnoreCase(text));
            where.and(event.description.containsIgnoreCase(text));
        }
        if (categories != null) {
            where.and(event.category.id.in(categories));
        }
        if (paid != null) {
            where.and(event.paid.eq(paid));
        }
        if (rangeStart != null) {
            where.and(event.eventDate.after(LocalDateTime.parse(rangeStart, formatter)));
        }
        if (rangeEnd != null) {
            where.and(event.eventDate.before(LocalDateTime.parse(rangeEnd, formatter)));
        }
        if (rangeStart == null && rangeEnd == null) {
            where.and(event.eventDate.after(LocalDateTime.now()));
        }

        Iterable<Event> foundEvents = eventStorage.findAll(where, sortedAndPageable);


        for (Event foundEvent : foundEvents) {
            Integer confirmedRequests = 0;
            Integer views = 0;

            List<Integer> confirmedRequestsAndViews = getConfirmedRequestAndViews(foundEvent);
            confirmedRequests = confirmedRequestsAndViews.get(0);
            views = confirmedRequestsAndViews.get(1);

            if (onlyAvailable) {
                if (!foundEvent.getParticipantLimit().equals(confirmedRequests)) {
                    result.add(EventMapper.toEventShortDto(foundEvent, confirmedRequests, views));
                }
            } else {
                result.add(EventMapper.toEventShortDto(foundEvent, confirmedRequests, views));
            }
        }

        if (sort != null) {
            switch (sort) {
                case "EVENT_DATE":
                    result = result.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).collect(Collectors.toList());
                    break;
                case "VIEWS":
                    result = result.stream().sorted(Comparator.comparing(EventShortDto::getViews)).collect(Collectors.toList());
                    break;
                default:
                    throw new ValidationException("Сортировка " + sort + " не поддерживается");
            }
        }

        sendData1toStatistic(request);
        return result;
    }

    @Override
    public EventFullDto getPubliclyById(Integer eventId, HttpServletRequest request) {

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new NotFoundException("Событие с id " + eventId + " не опубликованно");
        }

        Integer confirmedRequests = null;
        Integer views = null;

        List<Integer> confirmedRequestsAndViews = getConfirmedRequestAndViews(event);
        confirmedRequests = confirmedRequestsAndViews.get(0);
        views = confirmedRequestsAndViews.get(1);


        sendData1toStatistic(request);

        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    public void checkUser(Integer userId) {
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    @Override
    public EventShortDto getById(Integer eventId) {

        if (participationRequestStorage.existsByEventId(eventId)) {
            EventWithConfirmedRequest eventWithConfirmedRequest = participationRequestStorage
                    .getEvenWithConfirmedRequests(eventId, Status.CONFIRMED);

            Integer views = getViews(eventWithConfirmedRequest.getPublishedOn(), eventId);
            return EventMapper.toEventShortDto(eventWithConfirmedRequest, views);
        } else {
            Event event = eventStorage.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

            Integer views = getViews(event.getPublishedOn(), eventId);
            return EventMapper.toEventShortDto(event, 0, views);
        }
    }

    public List<Integer> getConfirmedRequestAndViews(Event event) {
        Integer confirmedRequests = 0;
        Integer views = 0;

        if (participationRequestStorage.existsByEventId(event.getId())) {
            confirmedRequests = participationRequestStorage.countByEventIdAndStatus(event.getId(),
                    Status.CONFIRMED);
        }

        ResponseEntity<Object> result = eventViewStatsClient.getStats(event.getPublishedOn().format(formatter),
                LocalDateTime.now().format(formatter), new String[]{"/event/" + event.getId()}, false);

        if (result.hasBody()) {
            List<ViewStats> viewStatsList = (List<ViewStats>) result.getBody();
            if (viewStatsList != null && !viewStatsList.isEmpty()) {
                views = viewStatsList.get(0).getHits();
            }
        }

        List<Integer> list = new ArrayList<>();
        list.add(confirmedRequests);
        list.add(views);

        return list;
    }

    public Integer getViews(LocalDateTime publishedOn, Integer eventId) {
        Integer views = 0;

        ResponseEntity<Object> result = eventViewStatsClient.getStats(publishedOn.format(formatter),
                LocalDateTime.now().format(formatter), new String[]{"/event/" + eventId}, false);

        if (result.hasBody()) {
            List<ViewStats> viewStatsList = (List<ViewStats>) result.getBody();
            if (viewStatsList != null && !viewStatsList.isEmpty()) {
                views = viewStatsList.get(0).getHits();
            }
        }
        return views;
    }

    public Event updateEventByParameters(Event event, UpdateEventRequest request) {
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
        if (request.getLocation() != null) {
            if (request.getLocation().getId() == null) {
                Location newLocation = locationStorage.save(request.getLocation());
                event.setLocation(newLocation);
            } else {
                if (locationStorage.existsById(request.getLocation().getId())) {
                    event.setLocation(request.getLocation());
                } else {
                    throw new NotFoundException("Локация с id " + request.getLocation().getId() + " не найдена");
                }
            }
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getCategory() != null) {
            Category category = categoryStorage.findById(request.getCategory()).orElseThrow(() ->
                    new NotFoundException("Категория с id " + request.getCategory() + " не найдена"));
            event.setCategory(category);
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        return event;
    }

    void sendData1toStatistic(HttpServletRequest request) {

        EndpointHit endpointHit = EndpointHit.builder()
                .app("evm-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(String.valueOf(LocalDateTime.now()))
                .build();
        eventEndpointHitClient.create(endpointHit);
    }
}
