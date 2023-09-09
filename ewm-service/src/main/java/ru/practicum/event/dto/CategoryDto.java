package ru.practicum.event.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryDto {

    Integer id;

    @NotNull(message = "Необходимо указать название категории")
    @NotBlank(message = "Название категории не может состоять из пустой строки")
    @Size(min = 1, max = 50, message = "Размер названия категории не входит в диапазон от {min} до {max} символов")
    String name;
}
