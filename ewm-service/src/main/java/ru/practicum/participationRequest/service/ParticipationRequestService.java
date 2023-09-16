package ru.practicum.participationRequest.service;

import ru.practicum.participationRequest.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateResult;
import ru.practicum.participationRequest.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    ParticipationRequestDto create(Integer userId, Integer eventId);

    List<ParticipationRequestDto> get(Integer userId);

    ParticipationRequestDto cancel(Integer userId, Integer requestId);

    List<ParticipationRequestDto> getRequestsOnEvent(Integer userId, Integer eventId);

    EventRequestStatusUpdateResult changeRequestStatuses(Integer userId, Integer eventId,
                                                         EventRequestStatusUpdateRequest request);

}
