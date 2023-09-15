package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.storage.StatsStorage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements ru.practicum.service.StatsService {

    private final StatsStorage statsStorage;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    @Override
    public EndpointHit create(EndpointHit endpointHit) {

        log.info(" ++++++++++++++++++++++++++++++++++++++++Stats server service  create ++++ " + endpointHit + " +++");

        EndpointHit endpointHit1 =  StatsMapper.toEndpointHit(statsStorage.save(StatsMapper.toStats(endpointHit)));

        log.info("++++++++++++++++++++++++++++++ Saved ENDPOINT ++++++" + endpointHit1 + "+++++++++++++");

        return endpointHit1;
        //        return StatsMapper.toEndpointHit(statsStorage.save(StatsMapper.toStats(endpointHit)));
    }

    @Override
    public List<ViewStats> get(String start, String end, String[] uris, Boolean unique) {
        Boolean isAll = uris[0].equals("[all]");
        log.info(" ++++++++++++++++++++++++++++ Stats server service ++++++++++" + start + end + uris[0] + unique + "++++ isAll " + isAll +"+++++++++++++++");


        LocalDateTime startTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endTime = LocalDateTime.parse(end, formatter);

        if (uris[0].equals("all")) {  // Если uri изначально не был указан и ему присвоилось значение "all",
            // то выгружвется вся статистика

            if (unique) {
                return statsStorage.findStatsAllWithUniqueIp(startTime, endTime);
            } else {
                return statsStorage.findStatsAll(startTime, endTime);
            }
        } else {  // Иначе идёт выгрузка статистики согласно заданным uri
            if (unique) {
                return statsStorage.findStatsForUrisWithUniqueIp(uris, startTime, endTime);
            } else {
                return statsStorage.findStatsForUris(uris, startTime, endTime);
            }
        }
    }
}
