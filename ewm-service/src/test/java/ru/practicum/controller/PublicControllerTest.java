package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.event.dto.CategoryDto;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.user.dto.UserShortDto;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
            .location(Location.builder().lon(34.87F).let(57.457F).build())
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

    @Test
    void shouldGetEvents() throws Exception {
        List<EventShortDto> eventShortDtoList = new ArrayList<>();
        eventShortDtoList.add(eventShortDto1);
        eventShortDtoList.add(eventShortDto2);

        when(eventService.getPublicly(anyString(), any(), anyBoolean(), anyString(), anyString(), anyBoolean(),
                anyString(), anyInt(), anyInt())).thenReturn(eventShortDtoList);

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
        when(eventService.getPubliclyById(anyInt())).thenReturn(eventFullDto);

        mvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Integer.class))
                .andExpect(jsonPath("$.title", is(eventFullDto.getTitle()), String.class));
    }
}
