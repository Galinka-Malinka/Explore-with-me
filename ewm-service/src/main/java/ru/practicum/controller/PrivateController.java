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
import ru.practicum.participationRequest.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateController {

    private final EventService eventService;

    private final ParticipationRequestService participationRequestService;

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
                                         @RequestParam(value = "from",
                                                 required = false, defaultValue = "0") Integer from,
                                         @RequestParam(value = "size",
                                                 required = false, defaultValue = "10") Integer size) {

        return eventService.getByInitiator(userId, from, size);
    }

    @PatchMapping("/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable Integer userId,
                                    @PathVariable Integer eventId,
                                    @RequestBody UpdateEventRequest request) {
        return eventService.update(userId, eventId, request);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsOnEvent(@PathVariable Integer userId,
                                                      @PathVariable Integer eventId) {
        return participationRequestService.getRequestsOnEvent(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatuses(@PathVariable Integer userId,
                                                                @PathVariable Integer eventId,
                                                                @RequestBody EventRequestStatusUpdateRequest request) {
        return participationRequestService.changeRequestStatuses(userId, eventId, request);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Integer userId,
                                                 @RequestParam(value = "eventId") Integer eventId) {
        return participationRequestService.create(userId, eventId);
    }

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getRequestsByUser(@PathVariable Integer userId) {
        return participationRequestService.get(userId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Integer userId,
                                                 @PathVariable Integer requestId) {
        return participationRequestService.cancel(userId, requestId);
    }

}
