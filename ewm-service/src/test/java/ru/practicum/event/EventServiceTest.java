package ru.practicum.event;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ViewStats;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.service.CategoryService;
import ru.practicum.client.EventEndpointHitClient;
import ru.practicum.client.EventViewStatsClient;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserService;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Transactional
@SpringBootTest(
        properties = "db.name=test",
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class EventServiceTest {

    private final EntityManager entityManager;

    private final EventService eventService;

    private final CategoryService categoryService;

    private final UserService userService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @MockBean
    EventViewStatsClient eventViewStatsClient;

    @Test
    void shouldCreateEvent() {
        Category category = CategoryMapper.toCategory(createCategory(1));
        User user = UserMapper.toUser(createUser(1));

        NewEventDto newEventDto = NewEventDto.builder()
                .title("Event")
                .annotation("annotation for Event")
                .description("description for Event")
                .eventDate("2035-09-28 09:00:00")
                .location(Location.builder().lon(54.68F).lat(35.98F).build())
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .category(1)
                .build();

        eventService.create(1, newEventDto);

        TypedQuery<Event> query = entityManager.createQuery("Select e from Event e where e.id = :id", Event.class);

        Event event = query.setParameter("id", 1).getSingleResult();

        assertThat(event.getId(), notNullValue());
        assertThat(event.getId(), is(1));
        assertThat(event.getTitle(), equalTo(newEventDto.getTitle()));
        assertThat(event.getAnnotation(), equalTo(newEventDto.getAnnotation()));
        assertThat(event.getDescription(), equalTo(newEventDto.getDescription()));
        assertThat(event.getEventDate().format(formatter), equalTo(newEventDto.getEventDate()));
        assertThat(event.getLocation(), equalTo(newEventDto.getLocation()));
        assertThat(event.getPaid(), equalTo(newEventDto.getPaid()));
        assertThat(event.getParticipantLimit(), equalTo(newEventDto.getParticipantLimit()));
        assertThat(event.getRequestModeration(), equalTo(newEventDto.getRequestModeration()));
        assertThat(event.getCategory(), equalTo(category));
        assertThat(event.getInitiator().getEmail(), equalTo(user.getEmail()));
        assertThat(event.getCreatedOn(), notNullValue());
        assertThat(event.getPublishedOn(), nullValue());

        assertThrows(NotFoundException.class, () -> eventService.create(2, newEventDto),
                "Пользователь с id 2 не найден");

        newEventDto.setCategory(2);

        assertThrows(NotFoundException.class, () -> eventService.create(1, newEventDto),
                "Категория с id 2 не найдена");

    }

    @Test
    void shouldGetByInitiatorById() {
        createUser(1);
        createCategory(1);
        Event event = createEvent(1);

        EventFullDto eventPending = eventService.getByInitiatorById(1, 1);

        assertThat(eventPending.getId(), notNullValue());
        assertThat(eventPending.getId(), is(1));
        assertThat(eventPending.getTitle(), equalTo(event.getTitle()));
        assertThat(eventPending.getAnnotation(), equalTo(event.getAnnotation()));
        assertThat(eventPending.getDescription(), equalTo(event.getDescription()));
        assertThat(eventPending.getEventDate(), equalTo(event.getEventDate().format(formatter)));
        assertThat(eventPending.getLocation(), equalTo(event.getLocation()));
        assertThat(eventPending.getPaid(), equalTo(event.getPaid()));
        assertThat(eventPending.getParticipantLimit(), equalTo(event.getParticipantLimit()));
        assertThat(eventPending.getRequestModeration(), equalTo(event.getRequestModeration()));
        assertThat(eventPending.getCategory(), equalTo(CategoryMapper.toCategoryDto(event.getCategory())));
        assertThat(eventPending.getInitiator(), equalTo(UserMapper.toUserShortDto(event.getInitiator())));
        assertThat(eventPending.getCreatedOn(), equalTo(event.getCreatedOn().format(formatter)));
        assertThat(eventPending.getState(), equalTo(event.getState().toString()));
        assertThat(eventPending.getPublishedOn(), nullValue());
        assertThat(eventPending.getConfirmedRequests(), nullValue());
        assertThat(eventPending.getViews(), nullValue());

        assertThrows(NotFoundException.class, () -> eventService.getByInitiatorById(2, 1),
                "Пользователь с id 2 не найден");
        assertThrows(NotFoundException.class, () -> eventService.getByInitiatorById(1, 2),
                "Событие с id 2 не найдено");

        UpdateEventRequest updateEventRequestWithPublish = UpdateEventRequest.builder()
                .stateAction("PUBLISH_EVENT")
                .build();

        eventService.updateByAdmin(1, updateEventRequestWithPublish);

        List<ViewStats> viewStats = new ArrayList<>();
        ViewStats stats = ViewStats.builder()
                .uri("/event/1")
                .app("ewm-service")
                .hits(3)
                .build();
        viewStats.add(stats);

        when(eventViewStatsClient.getStats(anyString(), anyString(), ArgumentMatchers.any(), anyBoolean()))
                .thenReturn(ResponseEntity.accepted().body(viewStats));

        EventFullDto eventPublished = eventService.getByInitiatorById(1, 1);

        assertThat(eventPublished.getTitle(), equalTo(event.getTitle()));
        assertThat(eventPublished.getPublishedOn(), notNullValue());
        assertThat(eventPublished.getViews(), is(3));
        assertThat(eventPublished.getConfirmedRequests(), is(0));

    }

    @Test
    void shouldGetEventsByInitiator() {
        createUser(1);
        createCategory(1);
        Event event1 = createEvent(1);
        Event event2 = createEvent(2);

        List<EventShortDto> eventShortDtoList = eventService.getByInitiator(1, 0, 10);

        assertThat(eventShortDtoList, hasSize(2));
        assertThat(eventShortDtoList.get(0).getId(), is(event1.getId()));
        assertThat(eventShortDtoList.get(0).getTitle(), equalTo(event1.getTitle()));
        assertThat(eventShortDtoList.get(0).getAnnotation(), equalTo(event1.getAnnotation()));
        assertThat(eventShortDtoList.get(0).getEventDate(), equalTo(event1.getEventDate().format(formatter)));
        assertThat(eventShortDtoList.get(0).getPaid(), equalTo(event1.getPaid()));
        assertThat(eventShortDtoList.get(0).getConfirmedRequests(), nullValue());
        assertThat(eventShortDtoList.get(0).getViews(), nullValue());
        assertThat(eventShortDtoList.get(0).getInitiator(), equalTo(UserMapper.toUserShortDto(event1.getInitiator())));
        assertThat(eventShortDtoList.get(0).getCategory(), equalTo(CategoryMapper.toCategoryDto(event1.getCategory())));
        assertThat(eventShortDtoList.get(1).getId(), is(event2.getId()));
        assertThat(eventShortDtoList.get(1).getTitle(), equalTo(event2.getTitle()));
        assertThat(eventShortDtoList.get(1).getAnnotation(), equalTo(event2.getAnnotation()));
        assertThat(eventShortDtoList.get(1).getEventDate(), equalTo(event2.getEventDate().format(formatter)));
        assertThat(eventShortDtoList.get(1).getPaid(), equalTo(event2.getPaid()));
        assertThat(eventShortDtoList.get(1).getConfirmedRequests(), nullValue());
        assertThat(eventShortDtoList.get(1).getViews(), nullValue());
        assertThat(eventShortDtoList.get(1).getInitiator(), equalTo(UserMapper.toUserShortDto(event2.getInitiator())));
        assertThat(eventShortDtoList.get(1).getCategory(), equalTo(CategoryMapper.toCategoryDto(event2.getCategory())));

        createEvent(3);

        List<EventShortDto> eventShortDtoListWithRestriction = eventService.getByInitiator(1, 1, 1);

        assertThat(eventShortDtoListWithRestriction, hasSize(1));
        assertThat(eventShortDtoListWithRestriction.get(0).getId(), is(event2.getId()));

        assertThrows(NotFoundException.class, () -> eventService.getByInitiator(2, 1, 1),
                "Пользователь с id 2 не найден");
    }

    @Test
    void shouldUpdateEventByInitiator() {
        createUser(1);
        createCategory(1);
        createCategory(2);
        createEvent(1);

        UpdateEventRequest updateEventRequest = UpdateEventRequest.builder()
                .title("UpdatedTitle")
                .annotation("UpdatedAnnotation for Event")
                .description("UpdatedDescription for Event")
                .eventDate("2035-09-29 09:00:00")
                .location(Location.builder().lon(55.68F).lat(33.98F).build())
                .paid(true)
                .participantLimit(10)
                .category(2)
                .requestModeration(true)
                .stateAction("CANCEL_REVIEW")
                .build();

        eventService.update(1, 1, updateEventRequest);

        TypedQuery<Event> query = entityManager.createQuery("Select e from Event e where e.id = :id", Event.class);

        Event updatedEvent = query.setParameter("id", 1).getSingleResult();

        assertThat(updatedEvent.getId(), is(1));
        assertThat(updatedEvent.getTitle(), equalTo(updateEventRequest.getTitle()));
        assertThat(updatedEvent.getAnnotation(), equalTo(updateEventRequest.getAnnotation()));
        assertThat(updatedEvent.getDescription(), equalTo(updateEventRequest.getDescription()));
        assertThat(updatedEvent.getEventDate().format(formatter), equalTo(updateEventRequest.getEventDate()));
        assertThat(updatedEvent.getLocation(), equalTo(updateEventRequest.getLocation()));
        assertThat(updatedEvent.getPaid(), equalTo(updateEventRequest.getPaid()));
        assertThat(updatedEvent.getParticipantLimit(), equalTo(updateEventRequest.getParticipantLimit()));
        assertThat(updatedEvent.getCategory().getId(), equalTo(updateEventRequest.getCategory()));
        assertThat(updatedEvent.getRequestModeration(), equalTo(updateEventRequest.getRequestModeration()));
        assertThat(updatedEvent.getState(), equalTo(State.CANCELED));

        assertThrows(NotFoundException.class, () -> eventService.update(2, 1, updateEventRequest),
                "Пользователь с id 2 не найден");
        assertThrows(NotFoundException.class, () -> eventService.update(1, 2, updateEventRequest),
                "Событие с id 2 не найдено");

        createUser(2);
        assertThrows(NotFoundException.class, () -> eventService.update(2, 1, updateEventRequest),
                "Пользователь с id 2 не является инициатором события с id 1");

        updateEventRequest.setEventDate(LocalDateTime.now().plusHours(1).format(formatter));
        assertThrows(ValidationException.class, () -> eventService.update(1, 1, updateEventRequest),
                "Дата и время на которые намечено событие" +
                        " не может быть раньше, чем через два часа от текущего момента");

        updateEventRequest.setEventDate("2035-09-28 09:00:00");
        updateEventRequest.setLocation(Location.builder().id(99).lat(46.57F).lon(43.67F).build());
        assertThrows(NotFoundException.class, () -> eventService.update(1, 1, updateEventRequest),
                "Локация с id 99 не найдена");

        updateEventRequest.setLocation(Location.builder().lon(54.68F).lat(35.98F).build());
        updateEventRequest.setCategory(5);
        assertThrows(NotFoundException.class, () -> eventService.update(1, 1, updateEventRequest),
                "Категория с id 5 не найдена");

        updateEventRequest.setCategory(1);
        updateEventRequest.setStateAction("CANCELED");
        assertThrows(ConflictException.class, () -> eventService.update(1, 1, updateEventRequest),
                "Обновление состояния события пользователем на CANCELED не возможно");
    }

    @Test
    void shouldGetEventsByAdmin() {
        createCategory(1);
        createUser(1);
        Event event1 = createEvent(1);
        Event event2 = createEvent(2);
        createUser(2);
        NewEventDto newEventDto = NewEventDto.builder()
                .title("Event3")
                .annotation("annotation for Event3")
                .description("description for Event3")
                .eventDate("2024-09-28 09:00:00")
                .location(Location.builder().lon(54.68F).lat(35.98F).build())
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .category(1)
                .build();

        eventService.create(2, newEventDto);

        List<EventFullDto> eventFullDtoList = eventService.getByAdmin(null, null, null,
                null, null, 0, 10);

        assertThat(eventFullDtoList, hasSize(3));
        assertThat(eventFullDtoList.get(0).getTitle(), equalTo(event1.getTitle()));
        assertThat(eventFullDtoList.get(1).getTitle(), equalTo(event2.getTitle()));
        assertThat(eventFullDtoList.get(2).getTitle(), equalTo("Event3"));

        Integer[] users = new Integer[]{1, 2};
        String[] states = new String[]{"PENDING"};
        Integer[] categories = new Integer[]{1};
        String rangeStart = "2023-09-01 09:00:00";
        String rangeEnd = "2040-09-28 09:00:00";

        List<EventFullDto> eventFullDtoListWithParameters = eventService.getByAdmin(users, states, categories,
                rangeStart, rangeEnd, 0, 10);

        assertThat(eventFullDtoListWithParameters, hasSize(3));
        assertThat(eventFullDtoListWithParameters.get(0).getTitle(), equalTo(event1.getTitle()));
        assertThat(eventFullDtoListWithParameters.get(1).getTitle(), equalTo(event2.getTitle()));
        assertThat(eventFullDtoList.get(2).getTitle(), equalTo("Event3"));

        Integer[] usersWithOne = new Integer[]{1};
        List<EventFullDto> eventFullDtoListWithOneUser = eventService.getByAdmin(usersWithOne, null, null,
                null, null, 0, 10);

        assertThat(eventFullDtoListWithOneUser, hasSize(2));
        assertThat(eventFullDtoListWithOneUser.get(0).getTitle(), equalTo(event1.getTitle()));
        assertThat(eventFullDtoListWithOneUser.get(1).getTitle(), equalTo(event2.getTitle()));

        List<EventFullDto> eventFullDtoListWithRangeStart = eventService.getByAdmin(null, null, null,
                "2025-09-01 09:00:00", null, 0, 10);

        assertThat(eventFullDtoListWithRangeStart, hasSize(2));
        assertThat(eventFullDtoListWithRangeStart.get(0).getTitle(), equalTo(event1.getTitle()));
        assertThat(eventFullDtoListWithRangeStart.get(1).getTitle(), equalTo(event2.getTitle()));

        List<EventFullDto> eventFullDtoListWithRangeEnd = eventService.getByAdmin(null, null, null,
                null, "2030-09-01 09:00:00", 0, 10);

        assertThat(eventFullDtoListWithRangeEnd, hasSize(1));
        assertThat(eventFullDtoListWithRangeEnd.get(0).getTitle(), equalTo("Event3"));

        List<EventFullDto> eventFullDtoListWithRange = eventService.getByAdmin(null, null, null,
                null, null, 1, 1);

        assertThat(eventFullDtoListWithRange, hasSize(1));
        assertThat(eventFullDtoListWithRange.get(0).getTitle(), equalTo(event2.getTitle()));
    }

    @Test
    void shouldUpdateEventByAdmin() {
        createUser(1);
        createCategory(1);
        createCategory(2);
        createEvent(1);

        UpdateEventRequest updateEventRequest = UpdateEventRequest.builder()
                .title("UpdatedTitle")
                .annotation("UpdatedAnnotation for Event")
                .description("UpdatedDescription for Event")
                .eventDate("2035-09-29 09:00:00")
                .location(Location.builder().lon(55.68F).lat(33.98F).build())
                .paid(true)
                .participantLimit(10)
                .category(2)
                .requestModeration(true)
                .stateAction("PUBLISH_EVENT")
                .build();

        eventService.updateByAdmin(1, updateEventRequest);

        TypedQuery<Event> query = entityManager.createQuery("Select e from Event e where e.id = :id", Event.class);

        Event updatedEvent = query.setParameter("id", 1).getSingleResult();

        assertThat(updatedEvent.getId(), is(1));
        assertThat(updatedEvent.getTitle(), equalTo(updateEventRequest.getTitle()));
        assertThat(updatedEvent.getAnnotation(), equalTo(updateEventRequest.getAnnotation()));
        assertThat(updatedEvent.getDescription(), equalTo(updateEventRequest.getDescription()));
        assertThat(updatedEvent.getEventDate().format(formatter), equalTo(updateEventRequest.getEventDate()));
        assertThat(updatedEvent.getLocation(), equalTo(updateEventRequest.getLocation()));
        assertThat(updatedEvent.getPaid(), equalTo(updateEventRequest.getPaid()));
        assertThat(updatedEvent.getParticipantLimit(), equalTo(updateEventRequest.getParticipantLimit()));
        assertThat(updatedEvent.getCategory().getId(), equalTo(updateEventRequest.getCategory()));
        assertThat(updatedEvent.getRequestModeration(), equalTo(updateEventRequest.getRequestModeration()));
        assertThat(updatedEvent.getState(), equalTo(State.PUBLISHED));

        Event event2 = createEvent(2);

        UpdateEventRequest updateEventRequestWithCanceled = UpdateEventRequest.builder()
                .stateAction("REJECT_EVENT")
                .build();

        eventService.updateByAdmin(2, updateEventRequestWithCanceled);

        TypedQuery<Event> query2 = entityManager.createQuery("Select e from Event e where e.id = :id", Event.class);

        Event canceledEvent = query2.setParameter("id", 2).getSingleResult();
        assertThat(canceledEvent.getId(), is(2));
        assertThat(canceledEvent.getTitle(), equalTo(event2.getTitle()));
        assertThat(canceledEvent.getState(), equalTo(State.CANCELED));

        assertThrows(ConflictException.class, () ->
                        eventService.updateByAdmin(1, updateEventRequestWithCanceled),
                "Событие можно отклонить, только если оно еще не опубликовано");

        UpdateEventRequest updateEventRequestWithPublish = UpdateEventRequest.builder()
                .stateAction("PUBLISH_EVENT")
                .build();

        assertThrows(ConflictException.class, () ->
                        eventService.updateByAdmin(2, updateEventRequestWithPublish),
                "Событие можно публиковать, только если оно в состоянии ожидания публикации");

        assertThrows(NotFoundException.class, () ->
                        eventService.updateByAdmin(3, updateEventRequestWithPublish),
                "Событие с id 3 не найдено");
    }

    public CategoryDto createCategory(Integer id) {
        NewCategoryDto newCategoryDto = NewCategoryDto.builder().name("Category" + id).build();
        return categoryService.create(newCategoryDto);
    }

    public UserDto createUser(Integer id) {
        UserDto userDto = UserDto.builder()
                .id(id)
                .name("User" + id)
                .email("User" + id + "@mail.ru")
                .build();

        return userService.create(userDto);
    }

    public Event createEvent(Integer id) {
        NewEventDto newEventDto = NewEventDto.builder()
                .title("Event" + id)
                .annotation("annotation for Event" + id)
                .description("description for Event" + id)
                .eventDate("2035-09-28 09:00:00")
                .location(Location.builder().lon(54.68F).lat(35.98F).build())
                .paid(false)
                .participantLimit(0)
                .requestModeration(false)
                .category(1)
                .build();

        eventService.create(1, newEventDto);

        TypedQuery<Event> query = entityManager.createQuery("Select e from Event e where e.id = :id", Event.class);

        return query.setParameter("id", id).getSingleResult();
    }
}
