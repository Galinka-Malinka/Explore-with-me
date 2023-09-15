package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.service.CompilationService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.service.EventService;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.service.UserService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin")
@Slf4j
public class AdminController {

    private final UserService userService;

    private final EventService eventService;

    private final CategoryService categoryService;

    private final CompilationService compilationService;

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@RequestBody @Valid UserDto userDto) {

        log.info("Добавление админом нового пользователя {}", userDto);
        return userService.create(userDto);
    }

    @GetMapping("/users")
    public List<UserDto> getUsers(@RequestParam(value = "ids", required = false, defaultValue = "0") Integer[] ids,
                                  @RequestParam(value = "from", required = false, defaultValue = "0") Integer from,
                                  @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {

        log.info("Получение админом всех пользователей с выборкой по ids {}, from {}, size {}", ids, from, size);
        return userService.get(ids, from, size);
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Integer userId) {

        log.info("Удаление админом пользователя с id {}", userId);
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

        log.info("Получение админом событий по следующим параметрам: users {}, states {}, categories{}," +
                " rangeStart{}, rangeEnd{}, from {}, size {}", users, states, categories, rangeStart, rangeEnd, from, size);
        return eventService.getByAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
    }

    @PatchMapping("/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable Integer eventId,
                                    @RequestBody UpdateEventRequest request) {

        log.info("Редактирование администратором события с id {} на {}", eventId, request);
        return eventService.updateByAdmin(eventId, request);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@RequestBody @Valid NewCategoryDto newCategoryDto) {

        log.info("Создание категории {}", newCategoryDto);
        return categoryService.create(newCategoryDto);
    }

    @PatchMapping("/categories/{catId}")
    public CategoryDto updateCategory(@PathVariable Integer catId,
                                      @RequestBody @Valid CategoryDto categoryDto) {

        log.info("Обновление категории с id {} на {}", catId, categoryDto);
        return categoryService.update(catId, categoryDto);
    }

    @DeleteMapping("/categories/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Integer catId) {

        log.info("Удаление категории с id {}", catId);
        categoryService.delete(catId);
    }

    @PostMapping("/compilations")
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@RequestBody @Valid NewCompilationDto newCompilationDto) {

        log.info("Создание админом новой подборки {}", newCompilationDto);
        return compilationService.create(newCompilationDto);
    }

    @PatchMapping("/compilations/{compId}")
    public CompilationDto updateCompilation(@PathVariable Integer compId,
                                            @RequestBody @Valid UpdateCompilationRequest updateCompilationRequest) {

        log.info("Обновление админом подборки с id {} на {}", compId, updateCompilationRequest);
        return compilationService.update(compId, updateCompilationRequest);
    }

    @DeleteMapping("/compilations/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompilation(@PathVariable Integer compId) {

        log.info("Удаление админом подборки с id {}", compId);
        compilationService.delete(compId);
    }
}
