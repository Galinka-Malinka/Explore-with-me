package ru.practicum.comment;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.comment.dto.FullCommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.ShortCommentDto;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.model.Comment;
import ru.practicum.comment.service.CommentService;
import ru.practicum.event.State;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.service.UserService;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest(
        properties = "db.name=test",
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CommentServiceTest {

    private final EntityManager entityManager;

    private final CategoryService categoryService;

    private final UserService userService;

    private final EventService eventService;

    private final CommentService commentService;

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void shouldCreateComment() {
        createCategory(1);
        createUser(1);
        createEvent(1, 1);

        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Comment")
                .build();

        assertThrows(ConflictException.class, () -> commentService.create(1, 1, newCommentDto),
                "Нельзя добавить комментарий не опубликованному событию с id 1");

        publicationEvent(1);

        commentService.create(1, 1, newCommentDto);

        TypedQuery<Comment> query = entityManager.createQuery("Select c from Comment c where c.id = :id",
                Comment.class);

        Comment comment = query.setParameter("id", 1).getSingleResult();

        assertThat(comment.getId(), notNullValue());
        assertThat(comment.getId(), is(1));
        assertThat(comment.getEvent().getId(), is(1));
        assertThat(comment.getAuthor().getId(), is(1));
        assertThat(comment.getText(), equalTo(newCommentDto.getText()));
        assertThat(comment.getCreated(), notNullValue());

        assertThrows(NotFoundException.class, () -> commentService.create(2, 1, newCommentDto),
                "Пользователь с id 2 не найден");

        assertThrows(NotFoundException.class, () -> commentService.create(1, 2, newCommentDto),
                "Событие с id 2 не найдено");
    }

    @Test
    void shouldGetComments() {
        createCategory(1);
        createUser(1);
        createEvent(1, 1);
        publicationEvent(1);
        Comment comment1 = createComment(1, 1, 1);
        createUser(2);
        Comment comment2 = createComment(2, 2, 1);

        createEvent(2, 2);
        publicationEvent(2);
        Comment comment3 = createComment(3, 2, 2);
        Comment comment4 = createComment(4, 1, 2);

        List<FullCommentDto> allCommentsWithoutRange = commentService
                .getComments(null, null, null, null, null, 0, 10);

        assertThat(allCommentsWithoutRange, notNullValue());
        assertThat(allCommentsWithoutRange.size(), is(4));

        assertThat(allCommentsWithoutRange.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsWithoutRange.get(1), equalTo(CommentMapper.toFullCommentDto(comment2)));
        assertThat(allCommentsWithoutRange.get(2), equalTo(CommentMapper.toFullCommentDto(comment3)));
        assertThat(allCommentsWithoutRange.get(3), equalTo(CommentMapper.toFullCommentDto(comment4)));

        Integer[] event1 = new Integer[]{1};

        List<FullCommentDto> allCommentsFotEvent1 = commentService
                .getComments(event1, null, null, null, null, 0, 10);

        assertThat(allCommentsFotEvent1, notNullValue());

        assertThat(allCommentsFotEvent1.size(), is(2));

        assertThat(allCommentsFotEvent1.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsFotEvent1.get(1), equalTo(CommentMapper.toFullCommentDto(comment2)));

        Integer[] twoEvents = new Integer[]{1, 2};

        List<FullCommentDto> allCommentsFotTwoEvents = commentService
                .getComments(twoEvents, null, null, null, null, 0, 10);

        assertThat(allCommentsFotTwoEvents, notNullValue());

        assertThat(allCommentsFotTwoEvents.size(), is(4));

        assertThat(allCommentsFotTwoEvents.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsFotTwoEvents.get(1), equalTo(CommentMapper.toFullCommentDto(comment2)));
        assertThat(allCommentsFotTwoEvents.get(2), equalTo(CommentMapper.toFullCommentDto(comment3)));
        assertThat(allCommentsFotTwoEvents.get(3), equalTo(CommentMapper.toFullCommentDto(comment4)));


        Integer[] user1 = new Integer[]{1};

        List<FullCommentDto> allCommentsForUser1 = commentService
                .getComments(null, user1, null, null, null, 0, 10);

        assertThat(allCommentsForUser1, notNullValue());

        assertThat(allCommentsForUser1.size(), is(2));
        assertThat(allCommentsForUser1.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsForUser1.get(1), equalTo(CommentMapper.toFullCommentDto(comment4)));

        Integer[] twoUsers = new Integer[]{1, 2};

        List<FullCommentDto> allCommentsForTwoUsers = commentService
                .getComments(null, twoUsers, null, null, null, 0, 10);

        assertThat(allCommentsForTwoUsers, notNullValue());

        assertThat(allCommentsForTwoUsers.size(), is(4));
        assertThat(allCommentsForTwoUsers.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsForTwoUsers.get(1), equalTo(CommentMapper.toFullCommentDto(comment2)));
        assertThat(allCommentsForTwoUsers.get(2), equalTo(CommentMapper.toFullCommentDto(comment3)));
        assertThat(allCommentsForTwoUsers.get(3), equalTo(CommentMapper.toFullCommentDto(comment4)));

        List<FullCommentDto> allCommentsWithText = commentService
                .getComments(null, null, "Comment", null, null, 0, 10);

        assertThat(allCommentsWithText, notNullValue());

        assertThat(allCommentsWithText.size(), is(4));
        assertThat(allCommentsWithText.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsWithText.get(1), equalTo(CommentMapper.toFullCommentDto(comment2)));
        assertThat(allCommentsWithText.get(2), equalTo(CommentMapper.toFullCommentDto(comment3)));
        assertThat(allCommentsWithText.get(3), equalTo(CommentMapper.toFullCommentDto(comment4)));


        List<FullCommentDto> allCommentsWithRangeStartBeforComments = commentService
                .getComments(null, null, null, LocalDateTime.now().minusMinutes(5).format(formatter),
                        null, 0, 10);

        assertThat(allCommentsWithRangeStartBeforComments, notNullValue());

        assertThat(allCommentsWithRangeStartBeforComments.size(), is(4));
        assertThat(allCommentsWithRangeStartBeforComments.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsWithRangeStartBeforComments.get(1), equalTo(CommentMapper.toFullCommentDto(comment2)));
        assertThat(allCommentsWithRangeStartBeforComments.get(2), equalTo(CommentMapper.toFullCommentDto(comment3)));
        assertThat(allCommentsWithRangeStartBeforComments.get(3), equalTo(CommentMapper.toFullCommentDto(comment4)));


        List<FullCommentDto> allCommentsWithRangeStartAfterComments = commentService
                .getComments(null, null, null, LocalDateTime.now().plusMinutes(5).format(formatter),
                        null, 0, 10);

        assertThat(allCommentsWithRangeStartAfterComments, notNullValue());

        assertThat(allCommentsWithRangeStartAfterComments.size(), is(0));


        List<FullCommentDto> allCommentsWithRangeEndtAfterComments = commentService
                .getComments(null, null, null, null,
                        LocalDateTime.now().plusMinutes(5).format(formatter), 0, 10);

        assertThat(allCommentsWithRangeEndtAfterComments, notNullValue());

        assertThat(allCommentsWithRangeEndtAfterComments.size(), is(4));
        assertThat(allCommentsWithRangeEndtAfterComments.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsWithRangeEndtAfterComments.get(1), equalTo(CommentMapper.toFullCommentDto(comment2)));
        assertThat(allCommentsWithRangeEndtAfterComments.get(2), equalTo(CommentMapper.toFullCommentDto(comment3)));
        assertThat(allCommentsWithRangeEndtAfterComments.get(3), equalTo(CommentMapper.toFullCommentDto(comment4)));

        List<FullCommentDto> allCommentsWithRangeEndtBeforComments = commentService
                .getComments(null, null, null, null,
                        LocalDateTime.now().minusMinutes(5).format(formatter), 0, 10);

        assertThat(allCommentsWithRangeEndtBeforComments, notNullValue());

        assertThat(allCommentsWithRangeEndtBeforComments.size(), is(0));


        List<FullCommentDto> allCommentsWithTimeRange = commentService
                .getComments(null, null, null, LocalDateTime.now().minusMinutes(5).format(formatter),
                        LocalDateTime.now().plusMinutes(5).format(formatter), 0, 10);

        assertThat(allCommentsWithTimeRange, notNullValue());

        assertThat(allCommentsWithTimeRange.size(), is(4));
        assertThat(allCommentsWithTimeRange.get(0), equalTo(CommentMapper.toFullCommentDto(comment1)));
        assertThat(allCommentsWithTimeRange.get(1), equalTo(CommentMapper.toFullCommentDto(comment2)));
        assertThat(allCommentsWithTimeRange.get(2), equalTo(CommentMapper.toFullCommentDto(comment3)));
        assertThat(allCommentsWithTimeRange.get(3), equalTo(CommentMapper.toFullCommentDto(comment4)));


        List<FullCommentDto> allCommentsWithRangePage = commentService
                .getComments(null, null, null, null, null, 2, 2);

        assertThat(allCommentsWithRangePage, notNullValue());

        assertThat(allCommentsWithRangePage.size(), is(2));
        assertThat(allCommentsWithRangePage.get(0), equalTo(CommentMapper.toFullCommentDto(comment3)));
        assertThat(allCommentsWithRangePage.get(1), equalTo(CommentMapper.toFullCommentDto(comment4)));


        assertThrows(ConflictException.class, () -> commentService.getComments(null, null, null,
                        LocalDateTime.now().plusMinutes(5).format(formatter),
                        LocalDateTime.now().minusMinutes(5).format(formatter), 0, 10),
                "Окончание временного диапазона не может быть раньше его начала");
    }

    @Test
    void shouldGetCommentsByEvent() {
        createCategory(1);
        createUser(1);
        createEvent(1, 1);
        publicationEvent(1);
        Comment comment1 = createComment(1, 1, 1);
        createUser(2);
        Comment comment2 = createComment(2, 2, 1);

        createEvent(2, 2);
        publicationEvent(2);
        createComment(3, 2, 2);
        createComment(4, 1, 2);

        createEvent(3, 2);
        publicationEvent(3);

        List<ShortCommentDto> listCommentsForEvent1 = commentService.getCommentsByEvent(1);

        assertThat(listCommentsForEvent1, notNullValue());

        assertThat(listCommentsForEvent1.size(), is(2));
        assertThat(listCommentsForEvent1.get(0).getAuthorName(), equalTo(comment1.getAuthor().getName()));
        assertThat(listCommentsForEvent1.get(0).getText(), equalTo(comment1.getText()));
        assertThat(listCommentsForEvent1.get(0).getCreated(), notNullValue());
        assertThat(listCommentsForEvent1.get(1).getAuthorName(), equalTo(comment2.getAuthor().getName()));
        assertThat(listCommentsForEvent1.get(1).getText(), equalTo(comment2.getText()));
        assertThat(listCommentsForEvent1.get(1).getCreated(), notNullValue());

        List<ShortCommentDto> listCommentsForEvent3 = commentService.getCommentsByEvent(3);

        assertThat(listCommentsForEvent3, notNullValue());
        assertThat(listCommentsForEvent3.size(), is(0));

        assertThrows(NotFoundException.class, () -> commentService.getCommentsByEvent(4),
                "Событие с id 4 не найдено");
    }

    @Test
    void shouldDeleteComment() {
        createCategory(1);
        createUser(1);
        createEvent(1, 1);
        publicationEvent(1);
        Comment comment1 = createComment(1, 1, 1);
        createUser(2);
        Comment comment2 = createComment(2, 2, 1);

        createEvent(2, 2);
        publicationEvent(2);
        Comment comment3 = createComment(3, 2, 2);
        Comment comment4 = createComment(4, 1, 2);

        createEvent(3, 2);
        publicationEvent(3);
        Comment comment5 = createComment(5, 1, 3);

        Integer[] comments4 = new Integer[]{4};

        commentService.delete(comments4, null);

        TypedQuery<Comment> queryForComment4 = entityManager
                .createQuery("Select c from Comment c where c.id = :id", Comment.class);

        assertThrows(NoResultException.class, () -> queryForComment4.setParameter("id", 4).getSingleResult());

        Integer[] comments2And3 = new Integer[]{2, 3};

        commentService.delete(comments2And3, null);

        TypedQuery<Comment> queryForComment2And3 = entityManager
                .createQuery("Select c from Comment c where c.id = :id", Comment.class);

        assertThrows(NoResultException.class, () -> queryForComment2And3.setParameter("id", 2).getSingleResult());
        assertThrows(NoResultException.class, () -> queryForComment2And3.setParameter("id", 3).getSingleResult());

        Integer[] event1 = new Integer[]{1};

        commentService.delete(null, event1);

        TypedQuery<Comment> queryForComment1 = entityManager
                .createQuery("Select c from Comment c where c.id = :id", Comment.class);

        assertThrows(NoResultException.class, () -> queryForComment1.setParameter("id", 1).getSingleResult());

        commentService.delete(null, null);

        TypedQuery<Comment> queryForComment5 = entityManager
                .createQuery("Select c from Comment c where c.id = :id", Comment.class);

        assertThrows(NoResultException.class, () -> queryForComment5.setParameter("id", 5).getSingleResult());

        Comment comment6 = createComment(6, 1, 1);
        Integer[] comments6 = new Integer[]{6};

        commentService.delete(comments6, event1);

        TypedQuery<Comment> queryForComment6 = entityManager
                .createQuery("Select c from Comment c where c.id = :id", Comment.class);

        assertThrows(NoResultException.class, () -> queryForComment6.setParameter("id", 6).getSingleResult());
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

    public void publicationEvent(Integer eventId) {
        UpdateEventRequest updateEventRequest = UpdateEventRequest.builder()
                .stateAction(State.PUBLISH_EVENT.toString())
                .build();

        eventService.updateByAdmin(eventId, updateEventRequest);
    }

    public Comment createComment(Integer id, Integer userId, Integer eventId) {
        NewCommentDto newCommentDto = NewCommentDto.builder()
                .text("Comment " + id)
                .build();

        commentService.create(userId, eventId, newCommentDto);

        TypedQuery<Comment> query = entityManager.createQuery("Select c from Comment c where c.id = :id",
                Comment.class);

        return query.setParameter("id", id).getSingleResult();
    }
}

