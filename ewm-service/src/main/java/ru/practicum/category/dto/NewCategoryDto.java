package ru.practicum.category.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class NewCategoryDto {

    @JsonCreator
    public NewCategoryDto(@JsonProperty("name") String name) {
        this.name = name;
    }

    @NotNull
    @NotBlank
    @Size(min = 1, max = 50, message = "Размер названия категории не входит в диапазон от {min} до {max} символов")
    String name;
}
