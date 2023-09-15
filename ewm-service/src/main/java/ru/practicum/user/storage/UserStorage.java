package ru.practicum.user.storage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.user.model.User;

import java.util.List;

public interface UserStorage extends JpaRepository<User, Integer> {

    @Query("select u" +
            " from User as u" +
            " where u.id in ?1")
    List<User> findAllByIds(Integer[] ids, Pageable pageable);

    Boolean existsByName(String name);
    Boolean existsByEmail(String email);
}
