package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.storage.CategoryStorage;
import ru.practicum.event.storage.EventStorage;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryStorage categoryStorage;

    private final EventStorage eventStorage;

    @Override
    public CategoryDto create(NewCategoryDto newCategoryDto) {
        if (categoryStorage.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория " + newCategoryDto.getName() + " уже существует");
        }

            return CategoryMapper.toCategoryDto(categoryStorage.saveAndFlush(CategoryMapper.toCategory(newCategoryDto)));
    }

    @Override
    public CategoryDto update(Integer catId, CategoryDto categoryDto) {
        Category category = categoryStorage.findById(catId).orElseThrow(() ->
                new NotFoundException("Категория с id " + catId + " не найдена"));

        if (!category.getName().equals(categoryDto.getName()) && categoryStorage.existsByName(categoryDto.getName())) {
            throw new ConflictException("Категория с названием " + categoryDto.getName() + " уже существует");
        }

        category.setName(categoryDto.getName());

        try {
            return CategoryMapper.toCategoryDto(categoryStorage.saveAndFlush(category));
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyExistsException("Категория " + categoryDto.getName() + " уже существует");
        }
    }

    @Override
    public void delete(Integer catId) {
        if (!categoryStorage.existsById(catId)) {
            throw new NotFoundException("Категория с id " + catId + " не найдена");
        }

        if (eventStorage.existsByCategoryId(catId)) {
            throw new ConflictException("Категория с id " + catId + " не может быть удалена," +
                    " т.к. существуют входящие в неё события");
        }

        categoryStorage.deleteById(catId);
    }

    @Override
    public List<CategoryDto> get(Integer from, Integer size) {
        List<CategoryDto> categoryDtoList = new ArrayList<>();

        int page = from / size;

        Pageable sortedAndPageable =
                PageRequest.of(page, size, Sort.by("id"));

        Page<Category> categories = categoryStorage.findAll(sortedAndPageable);
        if (!categories.isEmpty()) {
            for (Category category : categories) {
                categoryDtoList.add(CategoryMapper.toCategoryDto(category));
            }
        }
        return categoryDtoList;
    }

    @Override
    public CategoryDto getById(Integer catId) {
        Category category = categoryStorage.findById(catId).orElseThrow(() ->
                new NotFoundException("Категория с id " + catId + " не найдена"));
        return CategoryMapper.toCategoryDto(category);
    }
}
