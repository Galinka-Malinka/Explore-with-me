package ru.practicum.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.user.model.User;
import ru.practicum.user.storage.UserStorage;

import javax.persistence.TypedQuery;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class UserStorageTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    UserStorage userStorage;

    @Test
    void shouldSaveUser() {
        User user = User.builder()
                .name("User")
                .email("User@mail.ru")
                .build();

        userStorage.saveAndFlush(user);

        TypedQuery<User> queryForUser =
                entityManager.getEntityManager().createQuery("Select u from User u where u.id = :id", User.class);

        User resultUser = queryForUser.setParameter("id", 1).getSingleResult();
        assertThat(resultUser.getId(), notNullValue());
        assertThat(resultUser.getId(), is(1));
        assertThat(resultUser.getName(), equalTo(user.getName()));
        assertThat(resultUser.getEmail(), equalTo(user.getEmail()));
    }

    @Test
    void shouldGetUsers() {
        User user1 = createUser(1);
        User user2 = createUser(2);

        Integer[] idsWithTwoIds = new Integer[]{1, 2};
        Integer[] idsWithOneId = new Integer[]{1};

        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());

        List<User> resultWithOneId = userStorage.findAllByIds(idsWithOneId, pageable);

        assertThat(resultWithOneId, notNullValue());
        assertThat(resultWithOneId.size(), is(1));
        assertThat(resultWithOneId.get(0), equalTo(user1));

        List<User> resultWithTwoId = userStorage.findAllByIds(idsWithTwoIds, pageable);

        assertThat(resultWithTwoId, notNullValue());
        assertThat(resultWithTwoId.size(), is(2));
        assertThat(resultWithTwoId.get(0), equalTo(user1));
        assertThat(resultWithTwoId.get(1), equalTo(user2));

        createUser(3);
        Pageable pageableWithRestrictions = PageRequest.of(1, 1, Sort.by("id").ascending());

        List<User> resultWithPageable = userStorage.findAll(pageableWithRestrictions).getContent();
        assertThat(resultWithPageable, notNullValue());
        assertThat(resultWithPageable.size(), is(1));
        assertThat(resultWithPageable.get(0), equalTo(user2));

    }

    public User createUser(Integer id) {
        User user = User.builder()
                .name("User" + id)
                .email("User" + id + "@mail.ru")
                .build();

        userStorage.saveAndFlush(user);

        TypedQuery<User> queryForUser =
                entityManager.getEntityManager().createQuery("Select u from User u where u.id = :id", User.class);

        return queryForUser.setParameter("id", id).getSingleResult();
    }
}
