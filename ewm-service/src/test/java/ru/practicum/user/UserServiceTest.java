package ru.practicum.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserService;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
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
public class UserServiceTest {

    private final EntityManager entityManager;

    private final UserService userService;

    @Test
    void shouldCreateUser() {

        UserDto userDto = UserDto.builder()
                .name("User")
                .email("User@mail.ru")
                .build();

        userService.create(userDto);

        TypedQuery<User> queryForUser =
                entityManager.createQuery("Select u from User u where u.id = :id", User.class);

        User user = queryForUser.setParameter("id", 1).getSingleResult();
        assertThat(user.getId(), notNullValue());
        assertThat(user.getId(), is(1));
        assertThat(user.getName(), equalTo(userDto.getName()));
        assertThat(user.getEmail(), equalTo(userDto.getEmail()));

        assertThrows(AlreadyExistsException.class, () -> userService.create(userDto),
                "Пользователь с " + userDto.getEmail() + " уже зарегистрирован");
    }


    @Test
    void shouldGetUsers() {

        User user1 = createUser(1);
        User user2 = createUser(2);
        Integer[] idsWithTwoIds = new Integer[]{1, 2};
        Integer[] idsWithOneId = new Integer[]{1};
        Integer[] idsWihDefaultValue = new Integer[]{0};

        List<UserDto> resultWithOneId = userService.get(idsWithOneId, 0, 10);

        assertThat(resultWithOneId, notNullValue());
        assertThat(resultWithOneId.size(), is(1));
        assertThat(resultWithOneId.get(0), equalTo(UserMapper.toUserDto(user1)));

        List<UserDto> resultWithTwoId = userService.get(idsWithTwoIds, 0, 10);

        assertThat(resultWithTwoId, notNullValue());
        assertThat(resultWithTwoId.size(), is(2));
        assertThat(resultWithTwoId.get(0), equalTo(UserMapper.toUserDto(user1)));
        assertThat(resultWithTwoId.get(1), equalTo(UserMapper.toUserDto(user2)));

        List<UserDto> resultWithDefaultValue = userService.get(idsWihDefaultValue, 0, 10);

        assertThat(resultWithDefaultValue, notNullValue());
        assertThat(resultWithDefaultValue.size(), is(2));
        assertThat(resultWithDefaultValue.get(0), equalTo(UserMapper.toUserDto(user1)));
        assertThat(resultWithDefaultValue.get(1), equalTo(UserMapper.toUserDto(user2)));

        createUser(3);

        List<UserDto> resultWithPageable = userService.get(idsWihDefaultValue, 1, 1);
        assertThat(resultWithPageable, notNullValue());
        assertThat(resultWithPageable.size(), is(1));
        assertThat(resultWithPageable.get(0), equalTo(UserMapper.toUserDto(user2)));
    }


    @Test
    void shouldDeleteUser() {

        User user = createUser(1);

        userService.delete(user.getId());

        Integer[] ids = new Integer[]{user.getId()};

        List<UserDto> result = userService.get(ids, 0, 10);

        assertThat(result, notNullValue());
        assertThat(result.isEmpty(), equalTo(true));

        assertThrows(NotFoundException.class, () -> userService.delete(user.getId()),
                "Пользователь с id " + user.getId() + " не найден");
    }

    public User createUser(Integer id) {
        UserDto userDto = UserDto.builder()
                .id(id)
                .name("User" + id)
                .email("User" + id + "@mail.ru")
                .build();

        userService.create(userDto);

        TypedQuery<User> queryForUser =
                entityManager.createQuery("Select u from User u where u.email = :email", User.class);
        return queryForUser.setParameter("email", userDto.getEmail()).getSingleResult();
    }
}
