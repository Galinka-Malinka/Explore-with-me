package ru.practicum.participationRequest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.State;
import ru.practicum.event.model.Event;
import ru.practicum.event.storage.EventStorage;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationRequest.Status;
import ru.practicum.participationRequest.dto.ParticipationRequestDto;
import ru.practicum.participationRequest.mapper.ParticipationRequestMapper;
import ru.practicum.participationRequest.model.ParticipationRequest;
import ru.practicum.participationRequest.storage.ParticipationRequestStorage;
import ru.practicum.user.model.User;
import ru.practicum.user.storage.UserStorage;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestStorage participationRequestStorage;

    private final UserStorage userStorage;

    private final EventStorage eventStorage;

    @Override
    public ParticipationRequestDto create(Integer userId, Integer eventId) {
        User user = userStorage.findById(userId).orElseThrow(() ->
                new NotFoundException("Пользователь с id " + userId + " не найден"));

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь с id " + userId +
                    " не может добавить запрос на участие в событии с id " + eventId +
                    ", т.к. является его инициатором");
        }

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        Integer confirmedRequests = 0;

        if (participationRequestStorage.existsByEventId(eventId)) {
            confirmedRequests = participationRequestStorage.countByEventIdAndStatus(event.getId(),
                    Status.CONFIRMED);
        }

        if (event.getParticipantLimit() != 0 && event.getParticipantLimit().equals(confirmedRequests)) {
            throw new ConflictException("Невозможно оставиьт заявку на участие в событии с id " + eventId +
                    ", т.к. уже достигнут лимит участников");
        }

        if (participationRequestStorage.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Пользователь с id " + userId +
                    " уже отправлял заявку на участие в событии с id " + eventId);
        }

        Status status;
        if (event.getRequestModeration()) {
            status = Status.PENDING;
        } else {
            status = Status.CONFIRMED;
        }

        ParticipationRequest participationRequest = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        return ParticipationRequestMapper.toParticipationRequestDto(
                participationRequestStorage.save(participationRequest));
    }

    @Override
    public List<ParticipationRequestDto> get(Integer userId) {
        checkUser(userId);

        List<ParticipationRequestDto> participationRequestDtoList = new ArrayList<>();

        List<ParticipationRequest> participationRequestList = participationRequestStorage.findAllByRequesterId(userId);

        for (ParticipationRequest participationRequest : participationRequestList) {
            participationRequestDtoList.add(ParticipationRequestMapper.toParticipationRequestDto(participationRequest));
        }

        return participationRequestDtoList;
    }

    @Override
    public ParticipationRequestDto cancel(Integer userId, Integer requestId) {
        checkUser(userId);

        ParticipationRequest participationRequest = participationRequestStorage.findById(requestId).orElseThrow(() ->
                new NotFoundException("Заявка на участие с id " + requestId + " не найдена"));

        if (!participationRequest.getRequester().getId().equals(userId)) {
            throw new ValidationException("Пользователь с id " + userId + " не может отменить заявку на участие с id " +
                    requestId + " , т.к. не является её создателем");
        }

        participationRequest.setStatus(Status.CANCELED);

        return ParticipationRequestMapper.toParticipationRequestDto(
                participationRequestStorage.save(participationRequest));
    }

    public void checkUser(Integer userId) {
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    public void checkEvent(Integer eventId) {
        if (!eventStorage.existsById(eventId)) {
            throw new NotFoundException("Событие с id " + eventId + " не найдено");
        }
    }
}
