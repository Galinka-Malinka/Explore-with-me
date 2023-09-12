package ru.practicum.category.service;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto create(NewCategoryDto newCategoryDto);

    CategoryDto update(Integer catId, CategoryDto categoryDto);

    void delete(Integer catId);

    CategoryDto getById(Integer catId);

    List<CategoryDto> get(Integer from, Integer size);
}
