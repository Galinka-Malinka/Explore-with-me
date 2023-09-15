package ru.practicum.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.storage.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserStorage userStorage;

    @Override
    public UserDto create(UserDto userDto) {
        User user = UserMapper.toUser(userDto);

        if (userStorage.existsByName(userDto.getName())) {
            throw new ConflictException("Пользватель с именем " + userDto.getName() + " уже существует");
        }

        if (userStorage.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Пользватель с email " + userDto.getName() + " уже существует");
        }

            return UserMapper.toUserDto(userStorage.save(user));
    }

    @Override
    public List<UserDto> get(Integer[] ids, Integer from, Integer size) {
        int page = from / size;

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        List<User> users;

        if (ids[0] == 0) {
            Page<User> userPage = userStorage.findAll(pageable);
            users = userPage.getContent();
        } else {
            users = userStorage.findAllByIds(ids, pageable);
        }
        return UserMapper.toUserDtoList(users);
    }

    @Override
    public void delete(Integer userId) {
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        userStorage.deleteById(userId);
    }
}
