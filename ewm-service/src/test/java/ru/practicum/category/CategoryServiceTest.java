package ru.practicum.category;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.service.CategoryService;
import ru.practicum.event.dto.NewEventDto;
import ru.practicum.event.model.Location;
import ru.practicum.event.service.EventService;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.service.UserService;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest(
        properties = "db.name=test",
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class CategoryServiceTest {

    private final EntityManager entityManager;

    private final CategoryService categoryService;

    private final EventService eventService;

    private final UserService userService;

    @Test
    void shouldCreateCategory() {
        NewCategoryDto newCategoryDto = NewCategoryDto.builder().name("Category").build();
        categoryService.create(newCategoryDto);
        TypedQuery<Category> query =
                entityManager.createQuery("Select c from Category c where c.name = :name", Category.class);

        Category category = query.setParameter("name", newCategoryDto.getName()).getSingleResult();

        assertThat(category.getId(), notNullValue());
        assertThat(category.getId(), is(1));
        assertThat(category.getName(), equalTo(newCategoryDto.getName()));

        assertThrows(AlreadyExistsException.class, () -> categoryService.create(newCategoryDto),
                "Категория " + newCategoryDto.getName() + " уже существует");
    }

    @Test
    void shouldUpdateCategory() {
        createCategory(1);

        CategoryDto categoryDtoForUpdating = CategoryDto.builder().id(1).name("UpdateCategory").build();
        categoryService.update(1, categoryDtoForUpdating);

        TypedQuery<Category> query =
                entityManager.createQuery("Select c from Category c where c.id = :id", Category.class);

        Category category = query.setParameter("id", 1).getSingleResult();

        assertThat(category.getId(), notNullValue());
        assertThat(category.getId(), is(1));
        assertThat(category.getName(), equalTo(categoryDtoForUpdating.getName()));

        assertThrows(NotFoundException.class, () -> categoryService.update(2, categoryDtoForUpdating),
                "Категория с id 2 не найдена");

        createCategory(2);

        assertThrows(AlreadyExistsException.class, () -> categoryService.update(2, categoryDtoForUpdating),
                "Категория " + categoryDtoForUpdating.getName() + " уже существует");
    }

    @Test
    void shouldGetCategoryById() {
        createCategory(1);
        TypedQuery<Category> query =
                entityManager.createQuery("Select c from Category c where c.id = :id", Category.class);

        Category category = query.setParameter("id", 1).getSingleResult();

        Category result = CategoryMapper.toCategory(categoryService.getById(1));

        assertThat(result, equalTo(category));

        assertThrows(NotFoundException.class, () -> categoryService.getById(2),
                "Категория с id 2 не найдена");
    }

    @Test
    void shouldDeleteCategory() {

        createCategory(1);

        categoryService.delete(1);

        assertThrows(NotFoundException.class, () -> categoryService.getById(1),
                "Категория с id 1 не найдена");

        assertThrows(NotFoundException.class, () -> categoryService.delete(1),
                "Категория с id 1 не найдена");

        createCategory(2);

        UserDto userDto = UserDto.builder()
                .id(1)
                .name("User")
                .email("User@mail.ru")
                .build();

        userService.create(userDto);

        NewEventDto newEventDto = NewEventDto.builder()
                .title("Event")
                .annotation("annotation for Event")
                .description("description for Event")
                .eventDate("2024-09-05 09:00:00")
                .paid(false)
                .location(Location.builder().lat(57.457F).lon(34.87F).build())
                .category(2)
                .participantLimit(0)
                .requestModeration(false)
                .build();

        eventService.create(1, newEventDto);

        assertThrows(ConflictException.class, () -> categoryService.delete(2),
                "Категория с id 2 не может быть удалена," +
                        " т.к. существуют входящие в неё события");
    }

    @Test
    void shouldGetCategories() {
        Category category1 = createCategory(1);
        Category category2 = createCategory(2);

        List<CategoryDto> categoryDtoList = categoryService.get(0, 10);

        assertThat(categoryDtoList, hasSize(2));
        assertThat(categoryDtoList.get(0), equalTo(CategoryMapper.toCategoryDto(category1)));
        assertThat(categoryDtoList.get(1), equalTo(CategoryMapper.toCategoryDto(category2)));

        createCategory(3);

        List<CategoryDto> categoryDtoListWithRange = categoryService.get(1, 1);

        assertThat(categoryDtoListWithRange, hasSize(1));
        assertThat(categoryDtoListWithRange.get(0), equalTo(CategoryMapper.toCategoryDto(category2)));

        List<CategoryDto> emptyList = categoryService.get(4, 2);

        assertThat(emptyList.isEmpty(), equalTo(true));
    }

    public Category createCategory(Integer id) {
        NewCategoryDto newCategoryDto = NewCategoryDto.builder().name("Category" + id).build();
        categoryService.create(newCategoryDto);
        TypedQuery<Category> query =
                entityManager.createQuery("Select c from Category c where c.id = :id", Category.class);

        return query.setParameter("id", id).getSingleResult();
    }
}
