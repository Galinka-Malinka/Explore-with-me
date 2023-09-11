package ru.practicum.event.service;

import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.dto.UpdateEventRequest;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateResult;
import ru.practicum.participationRequest.dto.ParticipationRequestDto;

import java.util.List;

public interface EventService {

    EventFullDto create(Integer userId, NewEventDto newEventDto);

    EventFullDto getByInitiatorById(Integer userId, Integer eventId);

    List<EventShortDto> getByInitiator(Integer userId, Integer from, Integer size);

    EventFullDto update(Integer userId, Integer eventId, UpdateEventRequest request);

    ParticipationRequestDto getRequestsOnEvent(Integer userId, Integer eventId);

    EventRequestStatusUpdateResult changeRequestStatuses(Integer userId, Integer eventId,
                                                         EventRequestStatusUpdateRequest request);

    List<EventFullDto> getByAdmin(Integer[] users, String[] states, Integer[] categories, String rangeStart,
                                  String rangeEnd, Integer from, Integer size);

    EventFullDto updateByAdmin(Integer eventId, UpdateEventRequest request);

    List<EventShortDto> getPublicly(String text, Integer[] categories, Boolean paid, String rangeStart, String rangeEnd,
                                    Boolean onlyAvailable, String sort, Integer from, Integer size);

    EventFullDto getPubliclyById(Integer eventId);

}
