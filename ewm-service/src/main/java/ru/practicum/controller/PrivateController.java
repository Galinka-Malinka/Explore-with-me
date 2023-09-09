package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.service.EventService;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateResult;
import ru.practicum.participationRequest.dto.ParticipationRequestDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateController {

    private final EventService eventService;

    @PostMapping("/events")  //Добавление нового события
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Integer userId,
                                    @RequestBody NewEventDto newEventDto) {

        return eventService.create(userId, newEventDto);
    }

    @GetMapping("/events/{eventId}")
    public EventFullDto getEventById(@PathVariable Integer userId,
                                     @PathVariable Integer eventId) {

        return eventService.getByInitiatorById(userId, eventId);
    }

    @GetMapping("/events")
    public List<EventShortDto> getEvents(@PathVariable Integer userId,
                                         @RequestParam(value = "start",
                                                 required = false, defaultValue = "0") Integer start,
                                         @RequestParam(value = "size",
                                                 required = false, defaultValue = "10") Integer size) {

        return eventService.getByInitiator(userId, start, size);
    }

    @PatchMapping("/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable Integer userId,
                                    @PathVariable Integer eventId,
                                    @RequestBody UpdateEventRequest request) {
        return eventService.update(userId, eventId, request);
    }

    @GetMapping("/events/{eventId}/requests")
    public ParticipationRequestDto getRequestsOnEvent(@PathVariable Integer userId,
                                                      @PathVariable Integer eventId) {
        return eventService.getRequestsOnEvent(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatuses(@PathVariable Integer userId,
                                                                @PathVariable Integer eventId,
                                                                @RequestBody EventRequestStatusUpdateRequest request) {
        return eventService.changeRequestStatuses(userId, eventId, request);
    }
}
