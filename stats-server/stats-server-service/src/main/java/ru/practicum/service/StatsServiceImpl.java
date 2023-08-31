package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.storage.StatsStorage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements ru.practicum.service.StatsService {

    private final StatsStorage statsStorage;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    @Override
    public EndpointHit create(EndpointHit endpointHit) {
        return StatsMapper.toEndpointHit(statsStorage.save(StatsMapper.toStats(endpointHit)));
    }

    @Override
    public List<ViewStats> get(String start, String end, String[] uris, Boolean unique) {

        LocalDateTime startTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endTime = LocalDateTime.parse(end, formatter);

        List<ViewStats> viewStatsList = new ArrayList<>();

        if (uris[0].equals("all")) {  // Если uri изначально не был указан и ему присвоилось значение "all",
            // то выгружвется вся статистика
            List<String> apps = statsStorage.findDistinctApp();


            for (String app : apps) {
                List<String> uriList = statsStorage.findDistinctUriByApp(app);

                for (String uri : uriList) {
                    Integer hits = getHits(unique, app, uri, startTime, endTime);
                    viewStatsList.add(ViewStats.builder()
                            .app(app)
                            .uri(uri)
                            .hits(hits)
                            .build());

                }
            }
        } else {  // Иначе идёт выгрузка статистики согласно заданным uri
            for (String uri : uris) {
                List<String> apps = statsStorage.findDistinctAppByUri(uri);

                for (String app : apps) {
                    Integer hits = getHits(unique, app, uri, startTime, endTime);
                    viewStatsList.add(ViewStats.builder()
                            .app(app)
                            .uri(uri)
                            .hits(hits)
                            .build());
                }
            }
        }
        return viewStatsList.stream().sorted(Comparator.comparing(ViewStats::getHits).reversed())
                .collect(Collectors.toList());
    }

    public Integer getHits(Boolean unique, String app, String uri, LocalDateTime startTime, LocalDateTime endTime) {
        Integer hits;
        if (unique) {
            hits = statsStorage.countDistinctIp(app, uri, startTime, endTime);
        } else {
            hits = statsStorage.countAllIp(app, uri, startTime, endTime);
        }
        return hits;
    }
}
