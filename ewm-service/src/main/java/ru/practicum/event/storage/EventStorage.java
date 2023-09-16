package ru.practicum.event.storage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.event.State;
import ru.practicum.event.model.Event;

import java.util.List;

public interface EventStorage extends JpaRepository<Event, Integer>, QuerydslPredicateExecutor<Event> {
    Boolean existsByCategoryId(Integer catId);

    List<Event> findAllByInitiatorId(Integer userId, Pageable sortedAndPageable);

    Boolean existsByIdAndState(Integer eventId, State state);
}
