package ru.practicum.event.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.event.model.Location;

public interface LocationStorage extends JpaRepository<Location, Integer> {
}
