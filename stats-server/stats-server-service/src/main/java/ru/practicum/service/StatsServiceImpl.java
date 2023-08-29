package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.storage.StatsStorage;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsStorage statsStorage;

    @Transactional
    @Override
    public EndpointHit create(EndpointHit endpointHit) {
        return StatsMapper.toEndpointHit(statsStorage.save(StatsMapper.toStats(endpointHit)));
    }

    @Override
    public List<ViewStats> get(String start, String end, String[] uris, Boolean unique) {

        List<ViewStats> viewStatsList = new ArrayList<>();

        for (String uri : uris) {
            List<String> apps = statsStorage.findDistinctAppByUri(uri);

            for (String app : apps) {
                Integer hits = null;
                if (unique) {
                    hits = statsStorage.countDistinctIpByAppAndUri(app, uri);
                } else {
                    hits = statsStorage.countIpByAppAndUri(app, uri);
                }
                viewStatsList.add(ViewStats.builder()
                        .app(app)
                        .uri(uri)
                        .hits(hits)
                        .build());
            }
        }
        return viewStatsList;
    }
}
