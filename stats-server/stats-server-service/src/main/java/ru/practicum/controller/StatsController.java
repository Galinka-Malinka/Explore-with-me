package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.service.StatsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatsController {
    private final StatsService statsService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHit create(@RequestBody EndpointHit endpointHit) {


        EndpointHit endpointHit1 = statsService.create(endpointHit);
        log.info("================================== Stats server controller create endpointHit1 " + endpointHit1 + "============");

        return  endpointHit1;
//        return statsService.create(endpointHit);
    }

    @GetMapping("/stats")
    public List<ViewStats> get(@RequestParam(value = "start") String start,
                               @RequestParam(value = "end") String end,
                               @RequestParam(value = "uris") String[] uris,
                               @RequestParam(value = "unique") Boolean unique) {
log.info("================================ Stats server controller get uris " + uris);
        List<ViewStats> viewStatsList =  statsService.get(start, end, uris, unique);

        log.info("================================== Stats server controller get viewStatsList " + viewStatsList + "============" );

        return viewStatsList;
       // return statsService.get(start, end, uris, unique);
    }
}
