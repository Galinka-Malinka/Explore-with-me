package ru.practicum.participationRequest.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.participationRequest.Status;
import ru.practicum.participationRequest.model.ParticipationRequest;

import java.util.List;

public interface ParticipationRequestStorage extends JpaRepository<ParticipationRequest, Integer> {

    Boolean existsByEventId(Integer eventId);

    Integer countByEventIdAndStatus(Integer eventId, Status status);

    Boolean existsByEventIdAndRequesterId(Integer eventId, Integer userId);

    List<ParticipationRequest> findAllByRequesterId(Integer requesterId);
}
