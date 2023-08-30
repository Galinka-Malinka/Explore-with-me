package ru.practicum.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping(path = "/stats")
@RequiredArgsConstructor
@Slf4j
@Validated
public class StatsController {

    private final ru.practicum.stats.StatsClient statsClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public ResponseEntity<Object> getStats(@NotNull @NotBlank @RequestParam(value = "start") String start,
                                           @NotNull @NotBlank @RequestParam(value = "end") String end,
                                           @RequestParam(value = "uris", required = false,
                                                   defaultValue = "all") String[] uris,
                                           @RequestParam(value = "unique", required = false,
                                                   defaultValue = "false") Boolean unique) {
        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            startTime = LocalDateTime.parse(start, formatter);
            endTime = LocalDateTime.parse(end, formatter);

        } catch (ParseException e) {
            throw new IllegalArgumentException("Не верный формат дат");
        }

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Начало диапазона статистики не может быть позже его окончания");
        }

        if (endTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Окончание диапазона не может быть в будущем");
        }

        log.info("Получение статистики с {} по {} для uris {} с учётом уникальных посещений {}",
                start, end, uris, unique);
        return statsClient.getStats(start, end, uris, unique);
    }

    //start - Дата и время начала диапазона за который нужно выгрузить статистику (в формате "yyyy-MM-dd HH:mm:ss")
    //end - Дата и время конца диапазона за который нужно выгрузить статистику (в формате "yyyy-MM-dd HH:mm:ss")
    //uris - Список uri для которых нужно выгрузить статистику
    //unique - Нужно ли учитывать только уникальные посещения (только с уникальным ip)
}
