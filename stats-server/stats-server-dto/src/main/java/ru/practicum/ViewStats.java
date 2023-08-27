package ru.practicum;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ViewStats {

    String app;   //Название сервиса

    String uri;   //URI сервиса

    Integer hits;   //Количество просмотров

}
