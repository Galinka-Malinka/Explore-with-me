package ru.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.EndpointHit;
import ru.practicum.model.Stats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StatsMapper {
    public static final DateTimeFormatter DATEFORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Stats toStats(EndpointHit endpointHit) {

        Integer id = endpointHit.getId() != null ? endpointHit.getId() : null;
        LocalDateTime timestamp = endpointHit.getTimestamp() != null
                ? LocalDateTime.parse(endpointHit.getTimestamp(), DATEFORMATTER) : null;

        return Stats.builder()
                .id(id)
                .app(endpointHit.getApp())
                .uri(endpointHit.getUri())
                .ip(endpointHit.getIp())
                .timestamp(timestamp)
                .build();
    }

    public static EndpointHit toEndpointHit(Stats stats) {
        return EndpointHit.builder()
                .id(stats.getId())
                .app(stats.getApp())
                .uri(stats.getUri())
                .ip(stats.getIp())
                .timestamp(stats.getTimestamp().format(DATEFORMATTER))
                .build();
    }
}
