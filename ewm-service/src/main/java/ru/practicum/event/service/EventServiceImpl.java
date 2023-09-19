package ru.practicum.event.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
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
import ru.practicum.comment.dto.ShortCommentDto;
import ru.practicum.comment.storage.CommentStorage;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventViewStatsClient eventViewStatsClient;

    private final EventEndpointHitClient eventEndpointHitClient;

    private final EventStorage eventStorage;

    private final UserStorage userStorage;

    private final CategoryStorage categoryStorage;

    private final LocationStorage locationStorage;

    private final ParticipationRequestStorage participationRequestStorage;

    private final CommentStorage commentStorage;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto create(Integer userId, NewEventDto newEventDto) {

        User user = userStorage.findById(userId).orElseThrow(() ->
                new NotFoundException("Пользователь с id " + userId + " не найден"));

        Category category = categoryStorage.findById(newEventDto.getCategory()).orElseThrow(() ->
                new NotFoundException("Категория с id " + newEventDto.getCategory() + " не найдена"));

        Location location = locationStorage.save(newEventDto.getLocation());

        Event event = eventStorage.save(EventMapper.toEvent(user, newEventDto, category, location));

        return EventMapper.toEventFullDto(event, 0, 0, null);
    }


    @Override
    public EventFullDto getEventFullDtoByUserId(Integer userId, Integer eventId) {
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
                List<ShortCommentDto> comments = getComments(eventId);
                return EventMapper.toEventFullDto(event, 0, views, comments);
            }
        } else {
            Event event = eventStorage.findById(eventId)
                    .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
            return EventMapper.toEventFullDto(event, null, null, null);
        }
    }

    @Override
    public List<EventShortDto> getEventShortDtosByUserId(Integer userId, Integer from, Integer size) {
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

    @Override
    @Transactional
    public EventFullDto update(Integer userId, Integer eventId, UpdateEventRequest request) {
        if (request.getTitle() != null && (request.getTitle().length() < 3 || request.getTitle().length() > 120)) {
            throw new ValidationException("Размер заголовка не входит в диапазон от 3 до 120 символов");
        }

        if (request.getAnnotation() != null && (request.getAnnotation().length() < 20
                || request.getAnnotation().length() > 2000)) {
            throw new ValidationException("Размер аннотации не входит в диапазон от 20 до 2000 символов");
        }

        if (request.getDescription() != null && (request.getDescription().length() < 20
                || request.getDescription().length() > 7000)) {
            throw new ValidationException("Размер описания не входит в диапазон от 20 до 7000 символов");
        }

        checkUser(userId);

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
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
            throw new ValidationException("Дата и время на которые намечено событие" +
                    " не может быть раньше, чем через два часа от текущего момента");
        }

        Event eventWithUpdatedParameters = updateEventByParameters(event, request);

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals(State.CANCEL_REVIEW.toString())) {
                eventWithUpdatedParameters.setState(State.CANCELED);
            } else if (request.getStateAction().equals(State.SEND_TO_REVIEW.toString())) {
                eventWithUpdatedParameters.setState(State.PENDING);
            } else {
                throw new ConflictException("Обновление состояния события пользователем на "
                        + request.getStateAction() + " не возможно");
            }
        }
        Event updatedEvent = eventStorage.save(eventWithUpdatedParameters);

        return EventMapper.toEventFullDto(updatedEvent, null, null, null);
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

        if (users != null && users[0] != 0) {
            where.and(event.initiator.id.in(users));
        }
        if (states != null) {
            where.and(event.state.stringValue().in(states));
        }
        if (categories != null && categories[0] != 0) {
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
            List<ShortCommentDto> comments = null;

            if (foundEvent.getState().equals(State.PUBLISHED)) {
                List<Integer> confirmedRequestsAndViews = getConfirmedRequestAndViews(foundEvent);
                confirmedRequests = confirmedRequestsAndViews.get(0);
                views = confirmedRequestsAndViews.get(1);
                comments = getComments(foundEvent.getId());
            }
            eventFullDtoList.add(EventMapper.toEventFullDto(foundEvent, confirmedRequests, views, comments));
        }
        return eventFullDtoList;
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Integer eventId, UpdateEventRequest request) {

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        LocalDateTime timeNow = LocalDateTime.now();

        if (request.getTitle() != null && (request.getTitle().length() < 3 || request.getTitle().length() > 120)) {
            throw new ValidationException("Размер заголовка не входит в диапазон от 3 до 120 символов");
        }
        if (request.getAnnotation() != null
                && (request.getAnnotation().length() < 20 || request.getAnnotation().length() > 2000)) {
            throw new ValidationException("Размер аннотации не входит в диапазон от 20 до 2000 символов");
        }
        if (request.getDescription() != null
                && (request.getDescription().length() < 20 || request.getDescription().length() > 7000)) {
            throw new ValidationException("Размер описания не входит в диапазон от 20 до 7000 символов");
        }
        if (event.getEventDate() != null && !event.getEventDate().minusHours(1).isAfter(timeNow)) {
            throw new ConflictException("Дата начала изменяемого события должна быть" +
                    " не ранее чем за час от даты публикации");
        }
        if (request.getEventDate() == null && event.getEventDate().isBefore(timeNow) ||
                request.getEventDate() != null && LocalDateTime.parse(request.getEventDate(), formatter)
                        .isBefore(timeNow)) {
            throw new ValidationException("Дата и время на которые намечено событие" +
                    " не может быть раньше текущего момента");
        }
        if (request.getStateAction() != null && request.getStateAction().equals(State.PUBLISH_EVENT.toString()) &&
                !event.getState().equals(State.PENDING)) {
            throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации");
        }
        if (request.getStateAction() != null && request.getStateAction().equals(State.REJECT_EVENT.toString()) &&
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
        if (request.getStateAction() != null && request.getStateAction().equals(State.REJECT_EVENT.toString())) {
            eventWithUpdatedParameters.setState(State.CANCELED);
        }
        Event updatedEvent = eventStorage.save(eventWithUpdatedParameters);

        return EventMapper.toEventFullDto(updatedEvent, null, null, null);
    }

    @Override
    public List<EventShortDto> getPublicly(String text, Integer[] categories, Boolean paid,
                                           String rangeStart, String rangeEnd, Boolean onlyAvailable, String sort,
                                           Integer from, Integer size, HttpServletRequest request) {

        if (rangeStart != null && rangeEnd != null
                && LocalDateTime.parse(rangeStart, formatter).isAfter(LocalDateTime.parse(rangeEnd, formatter))) {
            throw new ValidationException("Окончание временного диапазона не может быть раньше его начала");
        }

        List<EventShortDto> result = new ArrayList<>();

        int page = from / size;

        Pageable sortedAndPageable =
                PageRequest.of(page, size, Sort.by("id"));

        QEvent event = QEvent.event;

        List<BooleanExpression> parameters = new ArrayList<>();
        if (text != null) {
            parameters.add(event.annotation.containsIgnoreCase(text).or(event.description.containsIgnoreCase(text)));
        }
        if (categories != null) {
            parameters.add(event.category.id.in(categories));
        }
        if (paid != null) {
            parameters.add(event.paid.eq(paid));
        }
        if (rangeStart != null) {
            parameters.add(event.eventDate.after(LocalDateTime.parse(rangeStart, formatter)));
        }
        if (rangeEnd != null) {
            parameters.add(event.eventDate.before(LocalDateTime.parse(rangeEnd, formatter)));
        }
        if (rangeStart == null && rangeEnd == null) {
            parameters.add(event.eventDate.after(LocalDateTime.now()));
        }

        List<Event> events;

        if (parameters.isEmpty()) {
            events = eventStorage.findAll(sortedAndPageable).toList();
        } else {
            BooleanExpression readyParameters = parameters.stream().reduce(BooleanExpression::and).get();
            events = eventStorage.findAll(readyParameters, sortedAndPageable).toList();
        }

        for (Event foundEvent : events) {
            Integer confirmedRequests = 0;
            Integer views = 0;

            if (foundEvent.getState().equals(State.PUBLISHED)) {
                List<Integer> confirmedRequestsAndViews = getConfirmedRequestAndViews(foundEvent);
                confirmedRequests = confirmedRequestsAndViews.get(0);
                views = confirmedRequestsAndViews.get(1);
            }
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
                    result = result.stream().sorted(Comparator.comparing(EventShortDto::getEventDate))
                            .collect(Collectors.toList());
                    break;
                case "VIEWS":
                    result = result.stream().sorted(Comparator.comparing(EventShortDto::getViews))
                            .collect(Collectors.toList());
                    break;
                default:
                    throw new ValidationException("Сортировка " + sort + " не поддерживается");
            }
        }

        sendDataToStatistic(request);

        return result;
    }

    @Override
    public EventFullDto getPubliclyById(Integer eventId, HttpServletRequest request) {

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new NotFoundException("Событие с id " + eventId + " не опубликованно");
        }

        Integer confirmedRequests;
        Integer views;

        List<Integer> confirmedRequestsAndViews = getConfirmedRequestAndViews(event);

        confirmedRequests = confirmedRequestsAndViews.get(0);
        views = confirmedRequestsAndViews.get(1);

        List<ShortCommentDto> comments = commentStorage.findByEventId(eventId);

        sendDataToStatistic(request);

        return EventMapper.toEventFullDto(event, confirmedRequests, views, comments);
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

            Integer views = 0;

            if (event.getPublishedOn() != null) {
                views = getViews(event.getPublishedOn(), eventId);
            }

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

        ResponseEntity<Object> result = eventViewStatsClient
                .getStats(event.getPublishedOn().minusMinutes(1).format(formatter),
                        LocalDateTime.now().plusMinutes(1).format(formatter),
                        new String[]{"/events/" + event.getId()}, true);

        if (result.hasBody()) {
            List<Map> viewStatsList = (List<Map>) result.getBody();

            if (viewStatsList != null && !viewStatsList.isEmpty()) {

                ViewStats viewStats = mapToViewStats(viewStatsList.get(0));

                views = viewStats.getHits();
            }
        }

        List<Integer> list = new ArrayList<>();
        list.add(confirmedRequests);
        list.add(views);

        return list;
    }

    private ViewStats mapToViewStats(Map map) {
        return ViewStats.builder()
                .hits((Integer) map.get("hits"))
                .app((String) map.get("app"))
                .uri((String) map.get("uri"))
                .build();
    }

    public Integer getViews(LocalDateTime publishedOn, Integer eventId) {
        Integer views = 0;

        ResponseEntity<Object> result = eventViewStatsClient.getStats(publishedOn.minusMinutes(1).format(formatter),
                LocalDateTime.now().plusMinutes(1).format(formatter), new String[]{"/events/" + eventId}, false);

        if (result.hasBody()) {
            List<ViewStats> viewStatsList = (List<ViewStats>) result.getBody();
            if (viewStatsList != null && !viewStatsList.isEmpty()) {
                views = viewStatsList.get(0).getHits();
            }
        }
        return views;
    }

    public Event updateEventByParameters(Event event, UpdateEventRequest request) {

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
        if (request.getCategory() != null) {
            Category category = categoryStorage.findById(request.getCategory()).orElseThrow(() ->
                    new NotFoundException("Категория с id " + request.getCategory() + " не найдена"));
            event.setCategory(category);
        }
        return EventMapper.update(event, request);
    }

    void sendDataToStatistic(HttpServletRequest request) {

        EndpointHit endpointHit = EndpointHit.builder()
                .app("evm-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(formatter))
                .build();

        eventEndpointHitClient.create(endpointHit);
    }

    public List<ShortCommentDto> getComments(Integer eventId) {
        List<ShortCommentDto> comments = new ArrayList<>();

        if (commentStorage.existsByEventId(eventId)) {
            comments = commentStorage.findByEventId(eventId);
        }
        return comments;
    }
}
