package ru.practicum.user.server;

import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto create(UserDto userDto);

    List<UserDto> get(Integer[] ids, Integer from, Integer size);

    void delete(Integer userId);
}
