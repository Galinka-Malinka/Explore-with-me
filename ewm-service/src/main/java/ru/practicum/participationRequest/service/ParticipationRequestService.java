package ru.practicum.participationRequest.service;

import ru.practicum.participationRequest.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    ParticipationRequestDto create(Integer userId, Integer eventId);

    List<ParticipationRequestDto> get(Integer userId);

    ParticipationRequestDto cancel(Integer userId, Integer requestId);

}
