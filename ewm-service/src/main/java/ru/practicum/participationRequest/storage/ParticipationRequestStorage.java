package ru.practicum.participationRequest.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.event.dto.EventWithConfirmedRequest;
import ru.practicum.participationRequest.Status;
import ru.practicum.participationRequest.model.ParticipationRequest;

import java.util.List;
import java.util.Set;

public interface ParticipationRequestStorage extends JpaRepository<ParticipationRequest, Integer> {

    Boolean existsByEventId(Integer eventId);

    Integer countByEventIdAndStatus(Integer eventId, Status status);

    Boolean existsByEventIdAndRequesterId(Integer eventId, Integer userId);

    List<ParticipationRequest> findAllByRequesterId(Integer requesterId);

    List<ParticipationRequest> findAllByEventId(Integer eventId);

    List<ParticipationRequest> findAllByIdIn(Set<Integer> participationRequests);

    @Query("select new ru.practicum.event.dto.EventWithConfirmedRequest(" +
            "e.id," +
            " e.title," +
            " e.annotation," +
            " e.description," +
            " e.eventDate," +
            " e.location," +
            " e.paid," +
            " e.participantLimit," +
            " e.category," +
            " e.requestModeration," +
            " e.initiator," +
            " e.createdOn," +
            " e.publishedOn," +
            " e.state," +
            " count(pr.id))" +
            " from ParticipationRequest as pr" +
            " join pr.event as e" +
            " where e.id = ?1" +
            " and pr.status = ?2" +
            " group by e.id")
    EventWithConfirmedRequest getEvenWithConfirmedRequests(Integer eventId, Status status);
}
