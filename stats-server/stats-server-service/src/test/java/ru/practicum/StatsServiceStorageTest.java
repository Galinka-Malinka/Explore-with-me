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

        String[] uris1 = new String[]{"/events/1"};
        String[] uris2 = new String[]{"/events", "/events/1"};

        LocalDateTime startTime = LocalDateTime.parse("2021-09-06 11:00:23",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse("2023-07-06 11:00:23",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<ViewStats> statsAllList = statsStorage.findStatsAll(startTime, endTime);
        assertThat(statsAllList, notNullValue());
        assertThat(statsAllList.size(), is(2));
        assertThat(statsAllList.get(0), equalTo(new ViewStats("ewm-main-service", "/events/1", 3)));
        assertThat(statsAllList.get(1), equalTo(new ViewStats("ewm-main-service", "/events", 1)));

        List<ViewStats> statsAllListWithUniqueIp = statsStorage.findStatsAllWithUniqueIp(startTime, endTime);
        assertThat(statsAllListWithUniqueIp, notNullValue());
        assertThat(statsAllListWithUniqueIp.size(), is(2));
        assertThat(statsAllListWithUniqueIp.get(0),
                equalTo(new ViewStats("ewm-main-service", "/events/1", 2)));
        assertThat(statsAllListWithUniqueIp.get(1),
                equalTo(new ViewStats("ewm-main-service", "/events", 1)));

        List<ViewStats> statsListForUris1 = statsStorage.findStatsForUris(uris1, startTime, endTime);
        assertThat(statsListForUris1, notNullValue());
        assertThat(statsListForUris1.size(), is(1));
        assertThat(statsListForUris1.get(0), equalTo(new ViewStats("ewm-main-service", "/events/1", 3)));

        List<ViewStats> statsListForUris2 = statsStorage.findStatsForUris(uris2, startTime, endTime);
        assertThat(statsListForUris2, notNullValue());
        assertThat(statsListForUris2.size(), is(2));
        assertThat(statsListForUris2.get(0), equalTo(new ViewStats("ewm-main-service", "/events/1", 3)));
        assertThat(statsListForUris2.get(1), equalTo(new ViewStats("ewm-main-service", "/events", 1)));

        List<ViewStats> statsAllForUris2WithUniqueIp =
                statsStorage.findStatsForUrisWithUniqueIp(uris2, startTime, endTime);
        assertThat(statsAllForUris2WithUniqueIp, notNullValue());
        assertThat(statsAllForUris2WithUniqueIp.size(), is(2));
        assertThat(statsAllForUris2WithUniqueIp.get(0),
                equalTo(new ViewStats("ewm-main-service", "/events/1", 2)));
        assertThat(statsAllForUris2WithUniqueIp.get(1),
                equalTo(new ViewStats("ewm-main-service", "/events", 1)));

        LocalDateTime newStartTime = LocalDateTime.parse("2023-01-06 11:00:23",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime newEndTime = LocalDateTime.parse("2023-04-06 11:00:23",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<ViewStats> statsListForUris1WithRange = statsStorage.findStatsForUris(uris1, newStartTime, newEndTime);
        assertThat(statsListForUris1WithRange, notNullValue());
        assertThat(statsListForUris1WithRange.size(), is(1));
        assertThat(statsListForUris1WithRange.get(0),
                equalTo(new ViewStats("ewm-main-service", "/events/1", 1)));
    }
}
