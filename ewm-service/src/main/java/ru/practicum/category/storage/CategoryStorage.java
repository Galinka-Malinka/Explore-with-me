package ru.practicum.category.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.category.model.Category;

public interface CategoryStorage extends JpaRepository<Category, Integer> {
    Boolean existsByName(String name);
}
