package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.service.CompilationService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.mapper.UserMapper;
import ru.practicum.user.service.UserService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
public class AdminControllerTest {

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;

    @MockBean
    EventService eventService;

    @MockBean
    CategoryService categoryService;

    @MockBean
    CompilationService compilationService;
    @Autowired
    MockMvc mvc;

    private final UserDto userDto1 = UserDto.builder()
            .id(1)
            .name("User1")
            .email("User1@mail.ru")
            .build();

    private final UserDto userDto2 = UserDto.builder()
            .id(2)
            .name("User2")
            .email("User2@mail.ru")
            .build();

    private final EventFullDto eventFullDto1 = EventFullDto.builder()
            .id(1)
            .title("Event1")
            .annotation("annotation for event1")
            .description("description for event1")
            .eventDate("2023-10-01 09:00:00")
            .location(Location.builder().lon(55.754167F).lat(37.62F).build())
            .paid(false)
            .participantLimit(0)
            .requestModeration(false)
            .confirmedRequests(10)
            .category(CategoryDto.builder().id(1).name("concerts").build())
            .initiator(UserMapper.toUserShortDto(userDto1))
            .createdOn("2023-09-01 09:00:00")
            .publishedOn("2023-09-03 09:00:00")
            .state("PUBLISHED")
            .views(15)
            .build();

    private final EventFullDto eventFullDto2 = EventFullDto.builder()
            .id(2)
            .title("Event2")
            .annotation("annotation for event2")
            .description("description for event2")
            .eventDate("2023-12-01 09:20:00")
            .location(Location.builder().lon(55.754167F).lat(37.62F).build())
            .paid(true)
            .participantLimit(50)
            .requestModeration(true)
            .confirmedRequests(27)
            .category(CategoryDto.builder().id(1).name("concerts").build())
            .initiator(UserMapper.toUserShortDto(userDto1))
            .createdOn("2023-09-05 09:00:00")
            .publishedOn("2023-09-07 09:00:00")
            .state("PUBLISHED")
            .views(30)
            .build();

    private final UpdateEventRequest updateEventRequest = UpdateEventRequest.builder().build();

    private final NewCategoryDto newCategoryDto = NewCategoryDto.builder()
            .name("Category")
            .build();

    private final CategoryDto categoryDto = CategoryDto.builder()
            .id(1)
            .name("Category")
            .build();

    private final NewCompilationDto newCompilationDto = NewCompilationDto.builder()
            .title("Title")
            .pinned(true)
            .events(new HashSet<>())
            .build();

    private final CompilationDto compilationDto = CompilationDto.builder()
            .title("Title")
            .pinned(true)
            .events(new HashSet<>())
            .build();

    private final UpdateCompilationRequest updateCompilationRequest = UpdateCompilationRequest.builder().build();

    @Test
    void shouldCreateUser() throws Exception {
        when(userService.create(any())).thenReturn(userDto1);

        mvc.perform(post("/admin/users").content(objectMapper.writeValueAsString(userDto1))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(userDto1.getId()), Integer.class))
                .andExpect(jsonPath("$.name", is(userDto1.getName()), String.class))
                .andExpect(jsonPath("$.email", is(userDto1.getEmail()), String.class));
    }

    @Test
    void shouldGetUsers() throws Exception {
        List<UserDto> userDtoList = new ArrayList<>();
        userDtoList.add(userDto1);
        userDtoList.add(userDto2);

        when(userService.get(any(), anyInt(), anyInt())).thenReturn(userDtoList);

        mvc.perform(get("/admin/users")
                        .param("from", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(userDto1.getId()), Integer.class))
                .andExpect(jsonPath("$[0].name", is(userDto1.getName()), String.class))
                .andExpect(jsonPath("$[0].email", is(userDto1.getEmail()), String.class))
                .andExpect(jsonPath("$[1].id", is(userDto2.getId()), Integer.class))
                .andExpect(jsonPath("$[1].name", is(userDto2.getName()), String.class))
                .andExpect(jsonPath("$[1].email", is(userDto2.getEmail()), String.class));
    }

    @Test
    void shouldGetEvents() throws Exception {
        List<EventFullDto> eventFullDtoList = new ArrayList<>();
        eventFullDtoList.add(eventFullDto1);
        eventFullDtoList.add(eventFullDto2);

        when(eventService.getByAdmin(any(), any(), any(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(eventFullDtoList);

        mvc.perform(get("/admin/events")
                        .param("users", "0")
                        .param("states", "all")
                        .param("categories", "0")
                        .param("rangeStart", "2022-09-05 09:00:00")
                        .param("rangeEnd", "2024-09-05 09:00:00")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(eventFullDto1.getId()), Integer.class))
                .andExpect(jsonPath("$[1].id", is(eventFullDto2.getId()), Integer.class));
    }

    @Test
    void shouldUpdateEvent() throws Exception {
        when(eventService.updateByAdmin(anyInt(), any())).thenReturn(eventFullDto1);
        mvc.perform(patch("/admin/events/1")
                        .content(objectMapper.writeValueAsString(updateEventRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto1.getId()), Integer.class));
    }

    @Test
    void shouldCreateCategory() throws Exception {
        when(categoryService.create(any())).thenReturn(categoryDto);

        mvc.perform(post("/admin/categories").content(objectMapper.writeValueAsString(newCategoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is(categoryDto.getName()), String.class));
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        when(categoryService.update(anyInt(), any())).thenReturn(categoryDto);

        mvc.perform(patch("/admin/categories/1").content(objectMapper.writeValueAsString(categoryDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(categoryDto.getName()), String.class));
    }

    @Test
    void shouldCreateCompilation() throws Exception {
        when(compilationService.create(any())).thenReturn(compilationDto);

        mvc.perform(post("/admin/compilations").content(objectMapper.writeValueAsString(newCompilationDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", equalTo(compilationDto.getTitle()), String.class))
                .andExpect(jsonPath("$.pinned", equalTo(compilationDto.getPinned()), Boolean.class))
                .andExpect(jsonPath("$.events", empty()));
    }

    @Test
    void shouldUpdateCompilation() throws Exception {
        when(compilationService.update(anyInt(), any())).thenReturn(compilationDto);

        mvc.perform(patch("/admin/compilations/1")
                        .content(objectMapper.writeValueAsString(updateCompilationRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo(compilationDto.getTitle()), String.class))
                .andExpect(jsonPath("$.pinned", equalTo(compilationDto.getPinned()), Boolean.class))
                .andExpect(jsonPath("$.events", empty()));
    }
}
