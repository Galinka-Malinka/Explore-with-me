package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.service.EventService;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin")
public class AdminController {

    private final UserService userService;

    private final EventService eventService;

    private final CategoryService categoryService;

    @PostMapping("/users")   //Добавление нового пользователя
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@RequestBody UserDto userDto) {
        return userService.create(userDto);
    }

    @GetMapping("/users")   //Получение информации о пользователях
    public List<UserDto> getUsers(@RequestParam(value = "ids", required = false, defaultValue = "0") Integer[] ids,
                                  @RequestParam(value = "from", required = false, defaultValue = "0") Integer from,
                                  @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        return userService.get(ids, from, size);
    }

    @DeleteMapping("/users/{userId}")   //Удаление пользователя
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Integer userId) {
        userService.delete(userId);
    }

    @GetMapping("/events")
    public List<EventFullDto> getEvents
            (@RequestParam(value = "users", required = false) Integer[] users,
             @RequestParam(value = "states", required = false) String[] states,
             @RequestParam(value = "categories", required = false) Integer[] categories,
             @RequestParam(value = "rangeStart", required = false) String rangeStart,
             @RequestParam(value = "rangeEnd", required = false) String rangeEnd,
             @RequestParam(value = "from", required = false, defaultValue = "0") Integer from,
             @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        return eventService.getByAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable Integer eventId,
                                    @RequestBody UpdateEventRequest request) {
        return eventService.updateByAdmin(eventId, request);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@RequestBody NewCategoryDto newCategoryDto) {
        return categoryService.create(newCategoryDto);
    }

    @PatchMapping("/categories/{catId}")
    public CategoryDto updateCategory(@PathVariable Integer catId,
                                      @RequestBody CategoryDto categoryDto) {
        return categoryService.update(catId, categoryDto);
    }

    @DeleteMapping("/categories/ {catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Integer catId) {
        categoryService.delete(catId);
    }
}
