package ru.practicum.participationRequest;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.event.State;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateResult;
import ru.practicum.participationRequest.dto.ParticipationRequestDto;
import ru.practicum.participationRequest.model.ParticipationRequest;
import ru.practicum.participationRequest.service.ParticipationRequestService;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserService;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.validation.ValidationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest(
        properties = "db.name=test",
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ParticipationRequestServiceTest {

    private final EntityManager entityManager;

    private final ParticipationRequestService participationRequestService;

    private final UserService userService;

    private final CategoryService categoryService;

    private final EventService eventService;

    @Test
    void shouldCreateParticipationRequest() {
        createCategory(1);
        createUser(1);
        Event event = createEvent(1, 1);
        User user = createUser(2);
        publicationEvent(1);

        participationRequestService.create(user.getId(), event.getId());

        TypedQuery<ParticipationRequest> query =
                entityManager.createQuery("Select p from ParticipationRequest p where p.id = :id",
                        ParticipationRequest.class);

        ParticipationRequest participationRequest = query.setParameter("id", 1).getSingleResult();

        assertThat(participationRequest.getId(), is(1));
        assertThat(participationRequest.getRequester(), equalTo(user));
        assertThat(participationRequest.getEvent(), equalTo(event));
        assertThat(participationRequest.getCreated(), notNullValue());
        assertThat(participationRequest.getStatus(), equalTo(Status.CONFIRMED));

        assertThrows(NotFoundException.class, () -> participationRequestService.create(3, event.getId()),
                "Пользователь с id 3 не найден");
        assertThrows(NotFoundException.class, () -> participationRequestService.create(user.getId(), 2),
                "Событие с id 2 не найдено");
        assertThrows(ConflictException.class, () -> participationRequestService.create(user.getId(), event.getId()),
                "Пользователь с id " + user.getId() +
                        " уже отправлял заявку на участие в событии с id " + event.getId());

        UpdateEventRequest updateEventRequestLimit = UpdateEventRequest.builder()
                .participantLimit(1)
                .build();

        eventService.updateByAdmin(1, updateEventRequestLimit);

        User user3 = createUser(3);
        assertThrows(ConflictException.class, () -> participationRequestService.create(user3.getId(), event.getId()),
                "Невозможно оставиьт заявку на участие в событии с id " + event.getId() +
                        ", т.к. уже достигнут лимит участников");

        // Проверка статуса запроса при обязательной модерации

        UpdateEventRequest updateEventRequestModeration = UpdateEventRequest.builder()
                .participantLimit(0)
                .requestModeration(true)
                .build();

        eventService.updateByAdmin(1, updateEventRequestModeration);

        participationRequestService.create(user3.getId(), event.getId());

        TypedQuery<ParticipationRequest> query2 =
                entityManager.createQuery("Select p from ParticipationRequest p where p.id = :id",
                        ParticipationRequest.class);

        ParticipationRequest participationRequest2 = query2.setParameter("id", 2).getSingleResult();

        assertThat(participationRequest2.getId(), is(2));
        assertThat(participationRequest2.getRequester(), equalTo(user3));
        assertThat(participationRequest2.getStatus(), equalTo(Status.PENDING));
    }

    @Test
    void shouldGetParticipationRequestsByRequester() {
        createCategory(1);
        createUser(1);
        Event event1 = createEvent(1, 1);
        publicationEvent(1);

        createUser(2);
        Event event2 = createEvent(2, 2);
        publicationEvent(2);

        User requester = createUser(3);
        createParticipationRequest(1, requester.getId(), event1.getId());
        createParticipationRequest(2, requester.getId(), event2.getId());

        List<ParticipationRequestDto> participationRequestDtoList = participationRequestService.get(requester.getId());

        assertThat(participationRequestDtoList, hasSize(2));
        assertThat(participationRequestDtoList.get(0).getEvent(), is(event1.getId()));
        assertThat(participationRequestDtoList.get(0).getRequester(), is(requester.getId()));
        assertThat(participationRequestDtoList.get(1).getEvent(), is(event2.getId()));
        assertThat(participationRequestDtoList.get(1).getRequester(), is(requester.getId()));

        assertThrows(NotFoundException.class, () -> participationRequestService.get(4),
                "Пользователь с id 4 не найден");

        List<ParticipationRequestDto> participationRequestDtoList2 = participationRequestService.get(1);
        assertThat(participationRequestDtoList2.isEmpty(), equalTo(true));
    }

    @Test
    void shouldCancelParticipationRequestDto() {
        createCategory(1);
        createUser(1);
        Event event1 = createEvent(1, 1);
        publicationEvent(1);
        User requester = createUser(2);
        createParticipationRequest(1, requester.getId(), event1.getId());

        participationRequestService.cancel(requester.getId(), 1);

        TypedQuery<ParticipationRequest> query =
                entityManager.createQuery("Select p from ParticipationRequest p where p.id = :id",
                        ParticipationRequest.class);

        ParticipationRequest participationRequest = query.setParameter("id", 1).getSingleResult();

        assertThat(participationRequest, notNullValue());
        assertThat(participationRequest.getId(), is(1));
        assertThat(participationRequest.getStatus(), equalTo(Status.CANCELED));

        createUser(3);
        assertThrows(ValidationException.class, () -> participationRequestService.cancel(3, 1),
                "Пользователь с id 2 не может отменить заявку на участие с id 1" +
                        " , т.к. не является её создателем");

        assertThrows(NotFoundException.class, () -> participationRequestService.cancel(4, 1),
                "Пользователь с id 4 не найден");
    }

    @Test
    void shouldGetParticipationRequestsByEvent() {
        createCategory(1);
        createUser(1);
        createEvent(1, 1);
        publicationEvent(1);

        createUser(2);
        createParticipationRequest(1, 2, 1);

        createUser(3);
        createParticipationRequest(2, 3, 1);

        List<ParticipationRequestDto> participationRequestDtoList =
                participationRequestService.getRequestsOnEvent(1, 1);

        assertThat(participationRequestDtoList, hasSize(2));
        assertThat(participationRequestDtoList.get(0).getRequester(), is(2));
        assertThat(participationRequestDtoList.get(1).getRequester(), is(3));

        createEvent(2, 1);
        publicationEvent(2);

        List<ParticipationRequestDto> participationRequestEmpty =
                participationRequestService.getRequestsOnEvent(1, 2);

        assertThat(participationRequestEmpty.isEmpty(), equalTo(true));

        assertThrows(ConflictException.class, () -> participationRequestService.getRequestsOnEvent(2, 1),
                "Пользователь с id 2 не может получить доступ к запросам на участие в событии с id 1," +
                        " т.к. он не является его инициатором");

        assertThrows(NotFoundException.class, () -> participationRequestService.getRequestsOnEvent(4, 1),
                "Пользователь с id 4 не найден");

        assertThrows(NotFoundException.class, () -> participationRequestService.getRequestsOnEvent(1, 3),
                "Событие с id 3 не найдено");
    }

    @Test
    void shouldChangeStatusesOfParticipationRequests() {
        createCategory(1);
        createUser(1);
        createEvent(1, 1);
        publicationEvent(1);

        createUser(2);
        createParticipationRequest(1, 2, 1);

        createUser(3);
        createParticipationRequest(2, 3, 1);

        Set<Integer> requestIds = new HashSet<>();
        requestIds.add(1);
        requestIds.add(2);

        EventRequestStatusUpdateRequest request = EventRequestStatusUpdateRequest.builder()
                .requestIds(requestIds)
                .status(Status.CONFIRMED.toString())
                .build();

        EventRequestStatusUpdateResult result = participationRequestService
                .changeRequestStatuses(1, 1, request);

        assertThat(result.getConfirmedRequests().size(), is(2));
        assertThat(result.getConfirmedRequests().get(0).getId(), is(1));
        assertThat(result.getConfirmedRequests().get(0).getStatus(), equalTo(Status.CONFIRMED.toString()));
        assertThat(result.getConfirmedRequests().get(1).getId(), is(2));
        assertThat(result.getConfirmedRequests().get(1).getStatus(), equalTo(Status.CONFIRMED.toString()));
        assertThat(result.getRejectedRequests().isEmpty(), equalTo(true));

        NewEventDto newEventDto2 = NewEventDto.builder()
                .title("Event2")
                .annotation("annotation for Event2")
                .description("description for Event2")
                .eventDate("2025-09-28 09:00:00")
                .location(Location.builder().lon(54.68F).lat(35.98F).build())
                .paid(true)
                .participantLimit(2)
                .requestModeration(true)
                .category(1)
                .build();

        eventService.create(1, newEventDto2);

        publicationEvent(2);

        createUser(4);

        createParticipationRequest(3, 2, 2);
        createParticipationRequest(4, 3, 2);
        createParticipationRequest(5, 4, 2);
        
        requestIds.clear();
        requestIds.add(3);
        
        EventRequestStatusUpdateRequest requestWithOneId = EventRequestStatusUpdateRequest.builder()
                .requestIds(requestIds)
                .status(Status.CONFIRMED.toString())
                .build();

        EventRequestStatusUpdateResult resultWithOneRequest = participationRequestService
                .changeRequestStatuses(1, 2, requestWithOneId);

        assertThat(resultWithOneRequest.getConfirmedRequests(), hasSize(1));
        assertThat(resultWithOneRequest.getConfirmedRequests().get(0).getId(), is(3));
        assertThat(resultWithOneRequest.getConfirmedRequests().get(0).getStatus(),
                equalTo(Status.CONFIRMED.toString()));

        assertThrows(ConflictException.class, () -> participationRequestService
                        .changeRequestStatuses(1, 2, requestWithOneId),
                "Статус можно изменить только у заявок," +
                        " находящихся в состоянии ожидания");

        requestIds.remove(3);
        requestIds.add(4);
        requestIds.add(5);

        EventRequestStatusUpdateRequest requestWithWrongStatus = EventRequestStatusUpdateRequest.builder()
                .requestIds(requestIds)
                .status("WrongStatus")
                .build();

        assertThrows(ValidationException.class, () -> participationRequestService
                        .changeRequestStatuses(1, 2, requestWithWrongStatus),
                "Команда WrongStatus не поддерживается");

        EventRequestStatusUpdateRequest requestExceedingLimit = EventRequestStatusUpdateRequest.builder()
                .requestIds(requestIds)
                .status(Status.CONFIRMED.toString())
                .build();

        EventRequestStatusUpdateResult resultExceedingLimit = participationRequestService
                .changeRequestStatuses(1, 2, requestExceedingLimit);

        assertThat(resultExceedingLimit.getConfirmedRequests(), hasSize(1));
        assertThat(resultExceedingLimit.getConfirmedRequests().get(0).getId(), is(4));
        assertThat(resultExceedingLimit.getConfirmedRequests().get(0).getStatus(),
                equalTo(Status.CONFIRMED.toString()));
        assertThat(resultExceedingLimit.getRejectedRequests(), hasSize(1));
        assertThat(resultExceedingLimit.getRejectedRequests().get(0).getId(), is(5));
        assertThat(resultExceedingLimit.getRejectedRequests().get(0).getStatus(),
                equalTo(Status.CANCELED.toString()));

        assertThrows(ConflictException.class, () -> participationRequestService
                        .changeRequestStatuses(2, 2, requestExceedingLimit),
                "Пользователь с id 2 не может получить доступ к запросам на участие в событии с id 1," +
                        " т.к. он не является его инициатором");

        assertThrows(NotFoundException.class, () -> participationRequestService
                        .changeRequestStatuses(5, 2, requestExceedingLimit),
                "Пользователь с id 5 не найден");

        assertThrows(NotFoundException.class, () -> participationRequestService
                        .changeRequestStatuses(1, 3, requestExceedingLimit),
                "Событие с id 3 не найдено");

        assertThrows(ConflictException.class, () -> participationRequestService
                        .changeRequestStatuses(1, 2, requestExceedingLimit),
                "Нельзя подтвердить запрос на участие," +
                        " т.к. уже достигнут лимит по колличеству участников в данном событии");
    }


    public void createCategory(Integer id) {
        NewCategoryDto newCategoryDto = NewCategoryDto.builder().name("Category" + id).build();
        categoryService.create(newCategoryDto);
    }

    public User createUser(Integer id) {
        UserDto userDto = UserDto.builder()
                .id(id)
                .name("User" + id)
                .email("User" + id + "@mail.ru")
                .build();

        userService.create(userDto);

        TypedQuery<User> query = entityManager.createQuery("Select u from User u where u.id = :id", User.class);

        return query.setParameter("id", id).getSingleResult();
    }

    public Event createEvent(Integer id, Integer userId) {
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

        eventService.create(userId, newEventDto);

        TypedQuery<Event> query = entityManager.createQuery("Select e from Event e where e.id = :id", Event.class);

        return query.setParameter("id", id).getSingleResult();
    }

    public ParticipationRequest createParticipationRequest(Integer id, Integer userId, Integer eventId) {
        participationRequestService.create(userId, eventId);

        TypedQuery<ParticipationRequest> query =
                entityManager.createQuery("Select p from ParticipationRequest p where p.id = :id",
                        ParticipationRequest.class);

        return query.setParameter("id", id).getSingleResult();
    }

    public void publicationEvent(Integer eventId) {
        UpdateEventRequest updateEventRequest = UpdateEventRequest.builder()
                .stateAction(State.PUBLISH_EVENT.toString())
                .build();

        eventService.updateByAdmin(eventId, updateEventRequest);
    }
}
