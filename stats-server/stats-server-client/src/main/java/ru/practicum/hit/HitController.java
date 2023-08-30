package ru.practicum.hit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.EndpointHit;

import javax.validation.Valid;

@Controller
@RequestMapping(path = "/hit")
@RequiredArgsConstructor
@Slf4j
@Validated
public class HitController {

    private final HitClient hitClient;

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED, reason = "Информация сохранена")
    public ResponseEntity<Object> create(@Valid @RequestBody EndpointHit endpointHit) {
        log.info("Сохранение в статистику запроса {}", endpointHit);
        return hitClient.create(endpointHit);
    }
}
