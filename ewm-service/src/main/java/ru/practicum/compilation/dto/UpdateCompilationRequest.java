package ru.practicum.compilation.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.Size;
import java.util.Set;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCompilationRequest {

    @Size(min = 1, max = 50, message = "Название подборки не входит в допустимый диапазон от {min} до {max} символов")
    String title;

    Boolean pinned;

    Set<Integer> events;

}
