package ru.practicum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.model.Stats;
import ru.practicum.storage.StatsStorage;

import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class StatsServiceStorageTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StatsStorage statsStorage;

    @Test
    void shouldSaveEndpointHit() {
        EndpointHit endpointHit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp("2022-09-06 11:00:23")
                .build();

        statsStorage.save(StatsMapper.toStats(endpointHit));

        TypedQuery<Stats> queryForItem = entityManager.getEntityManager().createQuery(
                "Select s from Stats s where s.id = :id",
                Stats.class);
        Stats stats = queryForItem.setParameter("id", 1).getSingleResult();

        assertThat(stats.getId(), notNullValue());
        assertThat(stats.getId(), is(1));
        assertThat(stats.getApp(), equalTo(endpointHit.getApp()));
        assertThat(stats.getUri(), equalTo(endpointHit.getUri()));
        assertThat(stats.getIp(), equalTo(endpointHit.getIp()));
        assertThat(stats.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                equalTo(endpointHit.getTimestamp()));
    }

    @Test
    void shouldGetStats() {

        EndpointHit endpointHit1 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp("2022-09-06 11:00:23")
                .build();

        EndpointHit endpointHit2 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events")
                .ip("192.163.0.1")
                .timestamp("2023-02-06 11:00:23")
                .build();

        EndpointHit endpointHit3 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp("2023-03-06 11:00:23")
                .build();

        EndpointHit endpointHit4 = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.2")
                .timestamp("2023-05-06 11:00:23")
                .build();

        statsStorage.save(StatsMapper.toStats(endpointHit1));
        statsStorage.save(StatsMapper.toStats(endpointHit2));
        statsStorage.save(StatsMapper.toStats(endpointHit3));
        statsStorage.save(StatsMapper.toStats(endpointHit4));

        List<String> resultDistinctAppByUri = statsStorage.findDistinctAppByUri("/events/1");

        assertThat(resultDistinctAppByUri, notNullValue());
        assertThat(resultDistinctAppByUri.size(), is(1));

        List<String> resultDistinctApp = statsStorage.findDistinctApp();

        assertThat(resultDistinctApp, notNullValue());
        assertThat(resultDistinctApp.size(), is(1));

        List<String> resultDistinctUriByApp = statsStorage.findDistinctUriByApp("ewm-main-service");

        assertThat(resultDistinctUriByApp, notNullValue());
        assertThat(resultDistinctUriByApp.size(), is(2));
        assertThat(resultDistinctUriByApp.get(0), equalTo("/events"));
        assertThat(resultDistinctUriByApp.get(1), equalTo("/events/1"));

        LocalDateTime startTime = LocalDateTime.parse("2021-09-06 11:00:23",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse("2023-07-06 11:00:23",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Integer countDistinctIp = statsStorage.countDistinctIp("ewm-main-service", "/events/1",
                startTime, endTime);

        assertThat(countDistinctIp, notNullValue());
        assertThat(countDistinctIp, is(2));

        Integer countAllIp = statsStorage.countAllIp("ewm-main-service", "/events/1",
                startTime, endTime);

        assertThat(countAllIp, notNullValue());
        assertThat(countAllIp, is(3));
    }
}
