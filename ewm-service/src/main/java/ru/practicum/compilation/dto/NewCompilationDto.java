package ru.practicum.compilation.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewCompilationDto {

    @NotNull(message = "Необходимо указать название подборки")
    @NotBlank(message = "Название подборки не может состоять из пустой строки")
    @Size(min = 1, max = 50, message = "Название подборки не входит в допустимый диапазон от {min} до {max} символов")
    String title;

    Boolean pinned;  //    Закреплена ли подборка на главной странице сайта (default: false)

    Set<Integer> events;  // Список идентификаторов событий входящих в подборку

}
