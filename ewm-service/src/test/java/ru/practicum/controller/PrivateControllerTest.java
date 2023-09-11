package ru.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateResult;
import ru.practicum.participationRequest.dto.ParticipationRequestDto;
import ru.practicum.user.dto.UserShortDto;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PrivateController.class)
public class PrivateControllerTest {
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

    private final NewEventDto newEventDto = NewEventDto.builder()
            .title("Event")
            .annotation("annotation for Event")
            .description("description for Event")
            .eventDate("2024-09-05 09:00:00")
            .paid(false)
            .location(Location.builder().lat(57.457F).lon(34.87F).build())
            .category(1)
            .participantLimit(0)
            .requestModeration(false)
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

    private final EventShortDto eventShortDto1 = EventShortDto.builder()
            .id(1)
            .annotation("annotation for Event")
            .eventDate("2024-09-05 09:00:00")
            .paid(false)
            .confirmedRequests(10)
            .views(15)
            .initiator(userShortDto)
            .category(CategoryDto.builder().id(1).name("concerts").build())
            .build();

    private final EventShortDto eventShortDto2 = EventShortDto.builder()
            .id(1)
            .annotation("annotation for Event2")
            .eventDate("2024-09-25 09:00:00")
            .paid(false)
            .confirmedRequests(12)
            .views(25)
            .initiator(userShortDto)
            .category(CategoryDto.builder().id(1).name("concerts").build())
            .build();

    private final ParticipationRequestDto participationRequestDto1 = ParticipationRequestDto.builder()
            .id(1)
            .event(1)
            .created("2023-09-07 09:00:00")
            .status("CONFIRMED")
            .requester(1)
            .build();

    private final ParticipationRequestDto participationRequestDto2 = ParticipationRequestDto.builder()
            .id(2)
            .event(1)
            .created("2023-09-08 09:00:00")
            .status("REJECTED")
            .requester(2)
            .build();

    private final UpdateEventRequest updateEventRequest = UpdateEventRequest.builder().build();
    private final EventRequestStatusUpdateRequest statusUpdateRequest = EventRequestStatusUpdateRequest.builder()
            .build();

    @Test
    void shouldCreateEvent() throws Exception {
        when(eventService.create(anyInt(), any())).thenReturn(eventFullDto);

        mvc.perform(post("/users/1/events")
                        .content(objectMapper.writeValueAsString(newEventDto))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Integer.class))
                .andExpect(jsonPath("$.title", is(eventFullDto.getTitle()), String.class));
    }

    @Test
    void shouldGetEventById() throws Exception {
        when(eventService.getByInitiatorById(anyInt(), anyInt())).thenReturn(eventFullDto);

        mvc.perform(get("/users/1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Integer.class))
                .andExpect(jsonPath("$.title", is(eventFullDto.getTitle()), String.class));
    }

    @Test
    void shouldGetEvents() throws Exception {
        List<EventShortDto> eventShortDtoList = new ArrayList<>();
        eventShortDtoList.add(eventShortDto1);
        eventShortDtoList.add(eventShortDto2);

        when(eventService.getByInitiator(anyInt(), anyInt(), anyInt())).thenReturn(eventShortDtoList);

        mvc.perform(get("/users/1/events")
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
    void shouldUpdateEvent() throws Exception {
        when(eventService.update(anyInt(), anyInt(), any())).thenReturn(eventFullDto);

        mvc.perform(patch("/users/1/events/1").content(objectMapper.writeValueAsString(updateEventRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(eventFullDto.getId()), Integer.class))
                .andExpect(jsonPath("$.title", is(eventFullDto.getTitle()), String.class));
    }

    @Test
    void shouldGetRequestOnEvent() throws Exception {
        when(eventService.getRequestsOnEvent(anyInt(), anyInt())).thenReturn(participationRequestDto1);

        mvc.perform(get("/users/1/events/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(participationRequestDto1.getId()), Integer.class))
                .andExpect(jsonPath("$.event", is(participationRequestDto1.getEvent()), Integer.class))
                .andExpect(jsonPath("$.requester", is(participationRequestDto1.getRequester()),
                        Integer.class));
    }

    @Test
    void shouldChangeRequestStatuses() throws Exception {
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        confirmedRequests.add(participationRequestDto1);

        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
        rejectedRequests.add(participationRequestDto2);

        EventRequestStatusUpdateResult result = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();

        when(eventService.changeRequestStatuses(anyInt(), anyInt(), any())).thenReturn(result);

        mvc.perform(patch("/users/1/events/1/requests")
                        .content(objectMapper.writeValueAsString(statusUpdateRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests.[0].id", is(participationRequestDto1.getId())))
                .andExpect(jsonPath("$.confirmedRequests.[0].event", is(participationRequestDto1.getEvent())))
                .andExpect(jsonPath("$.confirmedRequests.[0].status",
                        is(participationRequestDto1.getStatus())))
                .andExpect(jsonPath("$.rejectedRequests.[0].id", is(participationRequestDto2.getId())))
                .andExpect(jsonPath("$.rejectedRequests.[0].event", is(participationRequestDto2.getEvent())))
                .andExpect(jsonPath("$.rejectedRequests.[0].status",
                        is(participationRequestDto2.getStatus())));
    }
}
