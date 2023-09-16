package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comment.dto.FullCommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.service.CommentService;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.event.service.EventService;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateResult;
import ru.practicum.participationRequest.dto.ParticipationRequestDto;
import ru.practicum.participationRequest.service.ParticipationRequestService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
@Slf4j
public class PrivateController {

    private final EventService eventService;

    private final ParticipationRequestService participationRequestService;

    private final CommentService commentService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto createEvent(@PathVariable Integer userId,
                                    @RequestBody @Valid NewEventDto newEventDto) {

        log.info("Создание события {} пользователем с id {}", newEventDto, userId);
        return eventService.create(userId, newEventDto);
    }

    @GetMapping("/events/{eventId}")
    public EventFullDto getEventById(@PathVariable Integer userId,
                                     @PathVariable Integer eventId) {

        log.info("Получение события с id {} его оинициатором с id {}", eventId, userId);
        return eventService.getEventFullDtoByUserId(userId, eventId);
    }

    @GetMapping("/events")
    public List<EventShortDto> getEvents(@PathVariable Integer userId,
                                         @RequestParam(value = "from", required = false, defaultValue = "0")
                                         Integer from,
                                         @RequestParam(value = "size", required = false, defaultValue = "10")
                                         Integer size) {

        log.info("Получение всех событий их инициатором с id {} с ограничениями from {} и size {}", userId, from, size);
        return eventService.getEventShortDtosByUserId(userId, from, size);
    }

    @PatchMapping("/events/{eventId}")
    public EventFullDto updateEvent(@PathVariable Integer userId,
                                    @PathVariable Integer eventId,
                                    @RequestBody UpdateEventRequest request) {

        log.info("Изменение события с id {} его инициатором с id {} на {}", eventId, userId, request);
        return eventService.update(userId, eventId, request);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestsOnEvent(@PathVariable Integer userId,
                                                            @PathVariable Integer eventId) {

        log.info("Получение информации о запросах к событию с id {} его инициатором с id {}", eventId, userId);
        return participationRequestService.getRequestsOnEvent(userId, eventId);
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatuses(@PathVariable Integer userId,
                                                                @PathVariable Integer eventId,
                                                                @RequestBody EventRequestStatusUpdateRequest request) {

        log.info("Изменение статуса запросов на {} к событию с id {} инициатором с id {}", request, eventId, userId);
        return participationRequestService.changeRequestStatuses(userId, eventId, request);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Integer userId,
                                                 @RequestParam(value = "eventId") Integer eventId) {

        log.info("Созднание пользователем с id {} запроса на участие в событии с id {}", userId, eventId);
        return participationRequestService.create(userId, eventId);
    }

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getRequestsByUser(@PathVariable Integer userId) {

        log.info("Получение пользователем с id {} его запросов на участие в других событиях", userId);
        return participationRequestService.get(userId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Integer userId,
                                                 @PathVariable Integer requestId) {

        log.info("Отмена пользователем с id {} своего запроса на участие с id {}", userId, requestId);
        return participationRequestService.cancel(userId, requestId);
    }

    @PostMapping("/comments/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public FullCommentDto addComment(@PathVariable Integer userId, @PathVariable Integer eventId,
                                     @RequestBody @Valid NewCommentDto newCommentDto) {

        log.info("Добавление пользователем с id {} комментария {} событию с id {}", userId, newCommentDto, eventId);
        return commentService.create(userId, eventId, newCommentDto);
    }
}
