package ru.practicum.compilation.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.dto.EventShortDto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationDto {

    @NotNull
    Integer id;

    @NotNull(message = "Необходимо указать название подборки")
    @NotBlank(message = "Название подборки не может состоять из пустой строки")
    @Size(min = 1, max = 50, message = "Название подборки не входит в допустимый диапазон от {min} до {max} символов")
    String title;

    @NotNull
    Boolean pinned;  //Закреплена ли подборка на главной странице сайта

    Set<EventShortDto> events;  //Список событий входящих в подборку
}
