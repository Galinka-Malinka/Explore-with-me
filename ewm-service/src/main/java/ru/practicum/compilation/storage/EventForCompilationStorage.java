package ru.practicum.compilation.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.compilation.EventForCompilationPK;
import ru.practicum.compilation.model.EventForCompilation;

import java.util.Set;

public interface EventForCompilationStorage extends JpaRepository<EventForCompilation, EventForCompilationPK> {

    void deleteAllByEventForCompilationPKCompilationId(Integer compId);

    Set<EventForCompilation> findAllByEventForCompilationPKCompilationId(Integer compId);
}
