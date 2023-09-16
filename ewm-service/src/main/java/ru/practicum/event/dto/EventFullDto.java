package ru.practicum.event.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.model.Location;
import ru.practicum.user.dto.UserShortDto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {

    Integer id;

    @NotNull
    @NotBlank
    String title;

    @NotNull
    @NotBlank
    String annotation;

    String description;  //Полное описание события

    @NotNull
    @NotBlank
    String eventDate;  //Дата и время на которые намечено событие (в формате "yyyy-MM-dd HH:mm:ss")

    @NotNull
    Location location;

    @NotNull
    Boolean paid;  //Нужно ли оплачивать участие

    Integer participantLimit;  //Ограничение на количество участников. Значение 0 - означает отсутствие ограничения

    Boolean requestModeration;  //Нужна ли пре-модерация заявок на участие

    Integer confirmedRequests;  //Количество одобренных заявок на участие в данном событии

    @NotNull
    CategoryDto category;

    @NotNull
    UserShortDto initiator;

    String createdOn;  //Дата и время создания события (в формате "yyyy-MM-dd HH:mm:ss")

    String publishedOn;  //Дата и время публикации события (в формате "yyyy-MM-dd HH:mm:ss")

    String state;  //Список состояний жизненного цикла события Enum: [ PENDING, PUBLISHED, CANCELED ]

    Integer views;  //Количество просмотрев события
}

