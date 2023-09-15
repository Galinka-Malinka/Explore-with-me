package ru.practicum.compilation;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ViewStats;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.client.EventViewStatsClient;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.model.EventForCompilation;
import ru.practicum.compilation.service.CompilationService;
import ru.practicum.event.State;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationRequest.Status;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participationRequest.model.ParticipationRequest;
import ru.practicum.participationRequest.service.ParticipationRequestService;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserService;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class CompilationServiceTest {

    private final EntityManager entityManager;

    private final CompilationService compilationService;

    private final EventService eventService;

    private final CategoryService categoryService;

    private final UserService userService;

    private final ParticipationRequestService participationRequestService;

    @MockBean
    EventViewStatsClient eventViewStatsClient;

    @Test
    public void shouldCreateCompilation() {
        createCategory(1);
        createUser(1);
        createEvent(1, 1);
        publicationEvent(1);

        createUser(2);
        createParticipationRequest(1, 2, 1);
        confirmedRequest(1, 1, 1);

        List<ViewStats> viewStats = new ArrayList<>();
        ViewStats stats = ViewStats.builder()
                .uri("/event/1")
                .app("ewm-service")
                .hits(3)
                .build();
        viewStats.add(stats);

        when(eventViewStatsClient.getStats(anyString(), anyString(), ArgumentMatchers.any(), anyBoolean()))
                .thenReturn(ResponseEntity.accepted().body(viewStats));

        Set<Integer> events = new HashSet<>();
        events.add(1);

        NewCompilationDto newCompilationDto = NewCompilationDto.builder()
                .title("Title Collection")
                // .pinned(true)
                .events(events)
                .build();

        compilationService.create(newCompilationDto);

        TypedQuery<Compilation> query = entityManager
                .createQuery("Select c from Compilation c where c.id = :id", Compilation.class);

        Compilation compilation = query.setParameter("id", 1).getSingleResult();

        assertThat(compilation.getId(), notNullValue());
        assertThat(compilation.getId(), is(1));
        assertThat(compilation.getTitle(), equalTo(newCompilationDto.getTitle()));
        assertThat(compilation.isPinned(), equalTo(false));

        TypedQuery<EventForCompilation> queryForEvent = entityManager.createQuery("Select" +
                " ec from EventForCompilation ec join ec.eventForCompilationPK as pk" +
                " where pk.compilationId = :id", EventForCompilation.class);
        EventForCompilation eventForCompilation = queryForEvent.setParameter("id", 1).getSingleResult();

        assertThat(eventForCompilation.getEventForCompilationPK(), notNullValue());
        assertThat(eventForCompilation.getEventForCompilationPK().getCompilationId(), is(1));
        assertThat(eventForCompilation.getEventForCompilationPK().getEventId(), is(1));
    }

    @Test
    void shouldUpdateCompilation() {
        Set<Integer> events = createCompilation();

        events.remove(1);
        events.add(0);
        UpdateCompilationRequest updateCompilationRequest = UpdateCompilationRequest.builder()
                .title("UpdateTitle")
                .pinned(false)
                .events(events)
                .build();

        compilationService.update(1, updateCompilationRequest);

        TypedQuery<Compilation> query = entityManager
                .createQuery("Select c from Compilation c where c.id = :id", Compilation.class);

        Compilation compilation = query.setParameter("id", 1).getSingleResult();

        assertThat(compilation.getId(), notNullValue());
        assertThat(compilation.getId(), is(1));
        assertThat(compilation.getTitle(), equalTo(updateCompilationRequest.getTitle()));
        assertThat(compilation.isPinned(), equalTo(updateCompilationRequest.getPinned()));

        TypedQuery<EventForCompilation> queryForEvent = entityManager.createQuery("Select" +
                " ec from EventForCompilation ec join ec.eventForCompilationPK as pk" +
                " where pk.compilationId = :id", EventForCompilation.class);

        assertThrows(NoResultException.class, () -> queryForEvent.setParameter("id", 1).getSingleResult());

        //при отсутствии изменения названия и pinned и при добавлении событий
        events.remove(0);
        events.add(1);
        UpdateCompilationRequest updateCompilationRequest2 = UpdateCompilationRequest.builder()
                .events(events)
                .build();

        compilationService.update(1, updateCompilationRequest2);

        TypedQuery<Compilation> query2 = entityManager
                .createQuery("Select c from Compilation c where c.id = :id", Compilation.class);

        Compilation compilation2 = query2.setParameter("id", 1).getSingleResult();

        assertThat(compilation2.getId(), notNullValue());
        assertThat(compilation2.getId(), is(1));
        assertThat(compilation2.getTitle(), equalTo(updateCompilationRequest.getTitle()));
        assertThat(compilation2.isPinned(), equalTo(updateCompilationRequest.getPinned()));

        TypedQuery<EventForCompilation> queryForEvent2 = entityManager.createQuery("Select" +
                " ec from EventForCompilation ec join ec.eventForCompilationPK as pk" +
                " where pk.compilationId = :id", EventForCompilation.class);
        EventForCompilation eventForCompilation = queryForEvent2.setParameter("id", 1).getSingleResult();

        assertThat(eventForCompilation.getEventForCompilationPK(), notNullValue());
        assertThat(eventForCompilation.getEventForCompilationPK().getCompilationId(), is(1));
        assertThat(eventForCompilation.getEventForCompilationPK().getEventId(), is(1));

        //при отсутствии изменения событий подборки

        events.remove(1);
        UpdateCompilationRequest updateCompilationRequest3 = UpdateCompilationRequest.builder()
                .title("NewTitle")
                .pinned(true)
                .build();

        compilationService.update(1, updateCompilationRequest3);

        TypedQuery<Compilation> query3 = entityManager
                .createQuery("Select c from Compilation c where c.id = :id", Compilation.class);

        Compilation compilation3 = query3.setParameter("id", 1).getSingleResult();

        assertThat(compilation3.getId(), notNullValue());
        assertThat(compilation3.getId(), is(1));
        assertThat(compilation3.getTitle(), equalTo(updateCompilationRequest3.getTitle()));
        assertThat(compilation3.isPinned(), equalTo(updateCompilationRequest3.getPinned()));

        TypedQuery<EventForCompilation> queryForEvent3 = entityManager.createQuery("Select" +
                " ec from EventForCompilation ec join ec.eventForCompilationPK as pk" +
                " where pk.compilationId = :id", EventForCompilation.class);
        EventForCompilation eventForCompilationWithoutChange = queryForEvent3.setParameter("id", 1).getSingleResult();

        assertThat(eventForCompilationWithoutChange.getEventForCompilationPK(), notNullValue());
        assertThat(eventForCompilationWithoutChange.getEventForCompilationPK().getCompilationId(), is(1));
        assertThat(eventForCompilationWithoutChange.getEventForCompilationPK().getEventId(), is(1));
    }

    @Test
    void shouldDeleteCompilation() {
        createCompilation();

        compilationService.delete(1);

        TypedQuery<EventForCompilation> queryForEvent = entityManager.createQuery("Select" +
                " ec from EventForCompilation ec join ec.eventForCompilationPK as pk" +
                " where pk.compilationId = :id", EventForCompilation.class);

        assertThrows(NoResultException.class, () -> queryForEvent.setParameter("id", 1).getSingleResult());

        TypedQuery<Compilation> queryForCompilation = entityManager
                .createQuery("Select c from Compilation c where c.id = :id", Compilation.class);

        assertThrows(NoResultException.class, () -> queryForCompilation.setParameter("id", 1).getSingleResult());

        assertThrows(NotFoundException.class, () -> compilationService.delete(1),
                "Подборка с id 1 не найдена");
    }

    @Test
    void shouldGetCompilationById() {
        createCompilation();
        CompilationDto compilation = compilationService.getById(1);

        assertThat(compilation.getId(), notNullValue());
        assertThat(compilation.getId(), is(1));
        assertThat(compilation.getTitle(), equalTo("Title Collection"));
        assertThat(compilation.getPinned(), equalTo(true));
        assertThat(compilation.getEvents().size(), is(1));
    }

    @Test
    void shouldGetCompilations() {
        createCompilation();
        createEvent(2, 1);
        publicationEvent(2);
        createUser(3);
        createParticipationRequest(2, 3, 2);
        confirmedRequest(2, 1, 2);

        List<ViewStats> viewStats = new ArrayList<>();
        ViewStats stats = ViewStats.builder()
                .uri("/event/2")
                .app("ewm-service")
                .hits(3)
                .build();
        viewStats.add(stats);

        when(eventViewStatsClient.getStats(anyString(), anyString(), ArgumentMatchers.any(), anyBoolean()))
                .thenReturn(ResponseEntity.accepted().body(viewStats));

        Set<Integer> events = new HashSet<>();
        events.add(2);

        NewCompilationDto newCompilationDto = NewCompilationDto.builder()
                .title("Title Collection")
                .pinned(true)
                .events(events)
                .build();

        compilationService.create(newCompilationDto);

        List<CompilationDto> resultTruePinned = compilationService.get(true, 0, 10);
        assertThat(resultTruePinned, notNullValue());
        assertThat(resultTruePinned.size(), is(2));
        assertThat(resultTruePinned.get(0).getId(), is(1));
        assertThat(resultTruePinned.get(1).getId(), is(2));

        List<CompilationDto> resultFalsePinned = compilationService.get(false, 0, 10);
        assertThat(resultFalsePinned.size(), is(0));

        List<CompilationDto> resultWithoutRangePinned = compilationService.get(null, 0, 10);
        assertThat(resultWithoutRangePinned, notNullValue());
        assertThat(resultWithoutRangePinned.size(), is(2));
        assertThat(resultWithoutRangePinned.get(0).getId(), is(1));
        assertThat(resultWithoutRangePinned.get(1).getId(), is(2));

        List<CompilationDto> resultWithRangePage = compilationService.get(null, 1, 1);
        assertThat(resultWithRangePage, notNullValue());
        assertThat(resultWithRangePage.size(), is(1));
        assertThat(resultWithRangePage.get(0).getId(), is(2));
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

    public void confirmedRequest(Integer requestId, Integer initiatorId, Integer eventId) {
        Set<Integer> requestIds = new HashSet<>();
        requestIds.add(requestId);

        EventRequestStatusUpdateRequest request = EventRequestStatusUpdateRequest.builder()
                .requestIds(requestIds)
                .status(Status.CONFIRMED.toString())
                .build();

        participationRequestService.changeRequestStatuses(initiatorId, eventId, request);
    }

    public Set<Integer> createCompilation() {
        createCategory(1);
        createUser(1);
        createEvent(1, 1);
        publicationEvent(1);

        createUser(2);
        createParticipationRequest(1, 2, 1);
        confirmedRequest(1, 1, 1);

        List<ViewStats> viewStats = new ArrayList<>();
        ViewStats stats = ViewStats.builder()
                .uri("/event/1")
                .app("ewm-service")
                .hits(3)
                .build();
        viewStats.add(stats);

        when(eventViewStatsClient.getStats(anyString(), anyString(), ArgumentMatchers.any(), anyBoolean()))
                .thenReturn(ResponseEntity.accepted().body(viewStats));

        Set<Integer> events = new HashSet<>();
        events.add(1);

        NewCompilationDto newCompilationDto = NewCompilationDto.builder()
                .title("Title Collection")
                .pinned(true)
                .events(events)
                .build();

        compilationService.create(newCompilationDto);
        return events;
    }
}
