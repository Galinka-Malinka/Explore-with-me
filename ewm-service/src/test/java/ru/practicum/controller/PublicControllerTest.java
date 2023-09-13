package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.service.CategoryService;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.service.CompilationService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.user.dto.UserShortDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicController.class)
public class PublicControllerTest {
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    EventService eventService;

    @MockBean
    CategoryService categoryService;

    @MockBean
    CompilationService compilationService;

    @Autowired
    MockMvc mvc;

    private final UserShortDto userShortDto = UserShortDto.builder()
            .id(1)
            .name("User")
            .build();

    private final EventShortDto eventShortDto1 = EventShortDto.builder()
            .id(1)
            .title("Event1")
            .annotation("annotation for Event")
            .eventDate("2024-09-05 09:00:00")
            .paid(false)
            .confirmedRequests(10)
            .views(15)
            .initiator(userShortDto)
            .category(CategoryDto.builder().id(1).name("concerts").build())
            .build();

    private final EventShortDto eventShortDto2 = EventShortDto.builder()
            .id(2)
            .title("Event2")
            .annotation("annotation for Event2")
            .eventDate("2024-09-25 09:00:00")
            .paid(false)
            .confirmedRequests(12)
            .views(25)
            .initiator(userShortDto)
            .category(CategoryDto.builder().id(1).name("concerts").build())
            .build();

    private final EventFullDto eventFullDto = EventFullDto.builder()
            .id(1)
            .title("Event")
            .annotation("annotation for Event")
            .description("description for Event")
            .eventDate("2024-09-05 09:00:00")
            .location(Location.builder().lon(34.87F).lat(57.457F).build())
            .paid(false)
            .participantLimit(0)
            .requestModeration(false)
            .confirmedRequests(10)
            .category(CategoryDto.builder().id(1).name("concerts").build())
            .initiator(userShortDto)
            .createdOn("2023-09-01 09:00:00")
            .publishedOn("2023-09-03 09:00:00")
            .state("PUBLISHED")
            .views(15)
            .build();

    private final CategoryDto categoryDto1 = CategoryDto.builder()
            .id(1)
            .name("Category1")
            .build();

    private final CategoryDto categoryDto2 = CategoryDto.builder()
            .id(2)
            .name("Category2")
            .build();

    private final CompilationDto compilationDto1 = CompilationDto.builder()
            .title("Title")
            .pinned(true)
            .events(new HashSet<>())
            .build();

    private final CompilationDto compilationDto2 = CompilationDto.builder()
            .title("Title2")
            .pinned(true)
            .events(new HashSet<>())
            .build();

    @Test
    void shouldGetEvents() throws Exception {
        List<EventShortDto> eventShortDtoList = new ArrayList<>();
        eventShortDtoList.add(eventShortDto1);
        eventShortDtoList.add(eventShortDto2);

        when(eventService.getPublicly(anyString(), any(), anyBoolean(), anyString(), anyString(), anyBoolean(),
                anyString(), anyInt(), anyInt(), any())).thenReturn(eventShortDtoList);

        mvc.perform(get("/events")
                        .param("text", "annotation")
                        .param("categories", "0")
                        .param("paid", "false")
                        .param("rangeStart", "2023-09-25 09:00:00")
                        .param("rangeEnd", "2025-09-25 09:00:00")
                        .param("onlyAvailable", "false")
                        .param("sort", "EVENT_DATE")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(eventShortDto1.getId()), Integer.class))
                .andExpect(jsonPath("$[0].title", is(eventShortDto1.getTitle()), String.class))
                .andExpect(jsonPath("$[1].id", is(eventShortDto2.getId()), Integer.class))
                .andExpect(jsonPath("$[1].title", is(eventShortDto2.getTitle()), String.class));
    }

    @Test
    void shouldGeyEventById() throws Exception {
        when(eventService.getPubliclyById(anyInt(), any())).thenReturn(eventFullDto);

        mvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Integer.class))
                .andExpect(jsonPath("$.title", is(eventFullDto.getTitle()), String.class));
    }

    @Test
    void shouldGetCategoryById() throws Exception {
        when(categoryService.getById(anyInt())).thenReturn(categoryDto1);

        mvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(categoryDto1.getId()), Integer.class))
                .andExpect(jsonPath("$.name", is(categoryDto1.getName()), String.class));
    }

    @Test
    void shouldGetCategories() throws Exception {
        List<CategoryDto> categoryDtoList = new ArrayList<>();
        categoryDtoList.add(categoryDto1);
        categoryDtoList.add(categoryDto2);

        when(categoryService.get(anyInt(), anyInt())).thenReturn(categoryDtoList);

        mvc.perform(get("/categories")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(categoryDto1.getId()), Integer.class))
                .andExpect(jsonPath("$[0].name", is(categoryDto1.getName()), String.class))
                .andExpect(jsonPath("$[1].id", is(categoryDto2.getId()), Integer.class))
                .andExpect(jsonPath("$[1].name", is(categoryDto2.getName()), String.class));
    }

    @Test
    void shouldGetCompilationById() throws Exception {
        when(compilationService.getById(anyInt())).thenReturn(compilationDto1);

        mvc.perform(get("/compilations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo(compilationDto1.getTitle()), String.class))
                .andExpect(jsonPath("$.pinned", equalTo(compilationDto1.getPinned()), Boolean.class))
                .andExpect(jsonPath("$.events", empty()));
    }

    @Test
    void shouldGetCompilation() throws Exception {
        List<CompilationDto> result = new ArrayList<>();
        result.add(compilationDto1);
        result.add(compilationDto2);
        when(compilationService.get(anyBoolean(), anyInt(), anyInt())).thenReturn(result);

        mvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title", equalTo(compilationDto1.getTitle()), String.class))
                .andExpect(jsonPath("$[0].pinned", equalTo(compilationDto1.getPinned()), Boolean.class))
                .andExpect(jsonPath("$[0].events", empty()))
                .andExpect(jsonPath("$[1].title", equalTo(compilationDto2.getTitle()), String.class))
                .andExpect(jsonPath("$[1].pinned", equalTo(compilationDto2.getPinned()), Boolean.class))
                .andExpect(jsonPath("$[1].events", empty()));
    }
}
