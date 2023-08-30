package ru.practicum.stats;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.client.BaseClient;

import java.util.Map;

@Component
public class StatsClient extends BaseClient {

    private static final String API_PREFIX = "/stats";

    public StatsClient(@Value("${stats-server-service.url}") String serverUrl, RestTemplateBuilder builder) {
        super(
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                        .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                        .build()
        );
    }

    public ResponseEntity<Object> getStats(String start, String end, String[] uris, Boolean unique) {
        Map<String, Object> parameters = Map.of(
                    "start", start,
                    "end", end,
                    "uris", uris,
                    "unique", unique
            );

        return get("?start={start}&end={end}&uris={uris}&unique={unique}", null, parameters);
    }
}
