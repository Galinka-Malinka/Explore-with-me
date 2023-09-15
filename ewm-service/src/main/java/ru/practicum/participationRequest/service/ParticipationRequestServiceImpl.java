package ru.practicum.participationRequest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.State;
import ru.practicum.event.model.Event;
import ru.practicum.event.storage.EventStorage;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.participationRequest.Status;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateRequest;
import ru.practicum.participationRequest.dto.EventRequestStatusUpdateResult;
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
@Slf4j
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestStorage participationRequestStorage;

    private final UserStorage userStorage;

    private final EventStorage eventStorage;

    @Override
    public ParticipationRequestDto create(Integer userId, Integer eventId) {
        if (participationRequestStorage.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Пользователь с id " + userId +
                    " уже отправлял заявку на участие в событии с id " + eventId);
        }

        User user = userStorage.findById(userId).orElseThrow(() ->
                new NotFoundException("Пользователь с id " + userId + " не найден"));

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        log.info("#################################################3 event " + event);
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь с id " + userId +
                    " не может добавить запрос на участие в событии с id " + eventId +
                    ", т.к. является его инициатором");
        }

        if (!event.getState().equals(State.PUBLISHED)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (event.getParticipantLimit() == 0) {
            return ParticipationRequestMapper.toParticipationRequestDto(
                    participationRequestStorage.save(ParticipationRequest.builder()
                            .requester(user)
                            .event(event)
                            .created(LocalDateTime.now())
                            .status(Status.CONFIRMED)
                            .build()));
        }

        Integer confirmedRequests = 0;

        if (participationRequestStorage.existsByEventId(eventId)) {
            confirmedRequests = participationRequestStorage.countByEventIdAndStatus(event.getId(),
                    Status.CONFIRMED);
        }
log.info("#############################################################################33 (event.getParticipantLimit() " + event.getParticipantLimit() +
        " confirmedRequests " + confirmedRequests);
        if (event.getParticipantLimit() != 0 && event.getParticipantLimit().equals(confirmedRequests)) {
            throw new ConflictException("Невозможно оставиьт заявку на участие в событии с id " + eventId +
                    ", т.к. уже достигнут лимит участников");
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

    @Override
    public List<ParticipationRequestDto> getRequestsOnEvent(Integer userId, Integer eventId) {
        checkUser(userId);

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        checkUserIsInitiatorOfEvent(event, userId);

        List<ParticipationRequestDto> result = new ArrayList<>();

        List<ParticipationRequest> participationRequests = participationRequestStorage.findAllByEventId(eventId);

        if (!participationRequests.isEmpty()) {
            for (ParticipationRequest participationRequest : participationRequests) {
                result.add(ParticipationRequestMapper.toParticipationRequestDto(participationRequest));
            }
        }
        return result;
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatuses(Integer userId, Integer eventId,
                                                                EventRequestStatusUpdateRequest request) {
        checkUser(userId);

        Event event = eventStorage.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        checkUserIsInitiatorOfEvent(event, userId);

        List<ParticipationRequestDto> confirmedList = new ArrayList<>();
        List<ParticipationRequestDto> rejectedList = new ArrayList<>();
log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%55 event" + event);
        List<ParticipationRequest> participationRequestList = participationRequestStorage
                .findAllByIdIn(request.getRequestIds());
log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5participationRequestList" + participationRequestList);
        if (!participationRequestStorage.existsByEventId(eventId) || participationRequestList.isEmpty()) {
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(confirmedList)
                    .rejectedRequests(rejectedList)
                    .build();
        }

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            for (ParticipationRequest participationRequest : participationRequestList) {
                log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% CONFIRMED AUTOMATIC %%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                checkRequestOnExistsEvent(participationRequest, eventId);

                participationRequest.setStatus(Status.CONFIRMED);
                participationRequestStorage.save(participationRequest);

                confirmedList.add(ParticipationRequestMapper.toParticipationRequestDto(participationRequest));
            }
            return EventRequestStatusUpdateResult.builder()
                    .confirmedRequests(confirmedList)
                    .rejectedRequests(rejectedList)
                    .build();
        }
log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%55 NO AUTOMATIC %%%%%%%%%%%%%%%%%%%%%%%%%");
            Integer confirmedRequests = participationRequestStorage.countByEventIdAndStatus(event.getId(),
                    Status.CONFIRMED);
log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5  confirmedRequests  " + confirmedRequests);
            if (request.getStatus().equals(Status.CONFIRMED.toString())
                    && event.getParticipantLimit().equals(confirmedRequests)) {
                throw new ConflictException("Нельзя подтвердить запрос на участие," +
                        " т.к. уже достигнут лимит по колличеству участников в данном событии");
            }

            for (ParticipationRequest participationRequest : participationRequestList) {
                checkRequestOnExistsEvent(participationRequest, eventId);

                if (!participationRequest.getStatus().equals(Status.PENDING)) {
                    throw new ConflictException("Статус можно изменить только у заявок," +
                            " находящихся в состоянии ожидания");
                }

                if (request.getStatus().equals(Status.REJECTED.toString())) {

                    participationRequest.setStatus(Status.REJECTED);
                    participationRequestStorage.save(participationRequest);
                    rejectedList.add(ParticipationRequestMapper.toParticipationRequestDto(participationRequest));

                } else if (request.getStatus().equals(Status.CONFIRMED.toString())) {

                    if (!confirmedRequests.equals(event.getParticipantLimit())) {

                        participationRequest.setStatus(Status.CONFIRMED);
                        participationRequestStorage.save(participationRequest);
                        confirmedList.add(ParticipationRequestMapper.toParticipationRequestDto(participationRequest));

                        confirmedRequests++;
                    } else {
                        participationRequest.setStatus(Status.REJECTED);
                        participationRequestStorage.save(participationRequest);
                        rejectedList.add(ParticipationRequestMapper.toParticipationRequestDto(participationRequest));
                    }
                } else {
                    throw new ValidationException("Команда " + request.getStatus() + " не поддерживается");
                }
            }
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedList)
                .rejectedRequests(rejectedList)
                .build();
    }

    public void checkUser(Integer userId) {
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    public void checkRequestOnExistsEvent(ParticipationRequest participationRequest, Integer eventId) {
        if (!participationRequest.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Запрос с id " + participationRequest.getId() +
                    " не относится к событию с id " + eventId);
        }
    }

    public void checkUserIsInitiatorOfEvent(Event event, Integer userId) {
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь с id " + userId +
                    " не может получить доступ к запросам на участие в событии с id " + event.getId() +
                    ", т.к. он не является его инициатором");
        }
    }
}
