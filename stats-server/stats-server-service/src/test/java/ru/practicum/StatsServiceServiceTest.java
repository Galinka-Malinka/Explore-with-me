package ru.practicum;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.Stats;
import ru.practicum.service.StatsService;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Transactional
@SpringBootTest(
        properties = "db.name=test",
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class StatsServiceServiceTest {

    private final EntityManager em;
    private final StatsService statsService;

    @Test
    void shouldCreateEndpointHit() {
        EndpointHit endpointHit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp("2022-09-06 11:00:23")
                .build();

        statsService.create(endpointHit);

        TypedQuery<Stats> queryForItem = em.createQuery("Select s from Stats s where s.id = :id",
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
    void shouldCreateEndpointHitWithoutTimestamp() {
        EndpointHit endpointHit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .build();
        LocalDateTime timeNow = LocalDateTime.now();
        statsService.create(endpointHit);

        TypedQuery<Stats> queryForItem = em.createQuery("Select s from Stats s where s.id = :id",
                Stats.class);
        Stats stats = queryForItem.setParameter("id", 1).getSingleResult();
        assertThat(stats.getId(), notNullValue());
        assertThat(stats.getId(), is(1));
        assertThat(stats.getApp(), equalTo(endpointHit.getApp()));
        assertThat(stats.getUri(), equalTo(endpointHit.getUri()));
        assertThat(stats.getIp(), equalTo(endpointHit.getIp()));
        assertThat(stats.getTimestamp(), notNullValue());
        assertThat(stats.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                equalTo(timeNow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
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

        statsService.create(endpointHit1);
        statsService.create(endpointHit2);
        statsService.create(endpointHit3);
        statsService.create(endpointHit4);

        //Вывод статистики при отсутствии критериев к uri

        List<ViewStats> resultWithoutUris =
                statsService.get("2021-05-06 11:00:23", "2023-07-06 11:00:23",
                        null, false);

        assertThat(resultWithoutUris, notNullValue());
        assertThat(resultWithoutUris.size(), is(2));
        assertThat(resultWithoutUris.get(0).getApp(), equalTo("ewm-main-service"));
        assertThat(resultWithoutUris.get(0).getUri(), equalTo("/events"));
        assertThat(resultWithoutUris.get(0).getHits(), is(1));
        assertThat(resultWithoutUris.get(1).getApp(), equalTo("ewm-main-service"));
        assertThat(resultWithoutUris.get(1).getUri(), equalTo("/events/1"));
        assertThat(resultWithoutUris.get(1).getHits(), is(3));

        //Вывод статистики с списком uri

        List<ViewStats> resultWithAllUris =
                statsService.get("2021-05-06 11:00:23", "2023-07-06 11:00:23",
                        new String[]{"/events", "/events/1"}, false);

        assertThat(resultWithAllUris, notNullValue());
        assertThat(resultWithAllUris.size(), is(2));
        assertThat(resultWithAllUris, equalTo(resultWithoutUris));

        //Вывод статистики с 1 значением uri

        List<ViewStats> resultWithOneUris =
                statsService.get("2021-05-06 11:00:23", "2023-07-06 11:00:23",
                        new String[]{"/events/1"}, false);

        assertThat(resultWithOneUris, notNullValue());
        assertThat(resultWithOneUris.size(), is(1));
        assertThat(resultWithOneUris.get(0).getApp(), equalTo("ewm-main-service"));
        assertThat(resultWithOneUris.get(0).getUri(), equalTo("/events/1"));
        assertThat(resultWithOneUris.get(0).getHits(), is(3));

        //Вывод статистики с уникальными значениями ip

        List<ViewStats> resultWithUniqueIp =
                statsService.get("2021-05-06 11:00:23", "2023-07-06 11:00:23",
                        new String[]{"/events/1"}, true);

        assertThat(resultWithUniqueIp, notNullValue());
        assertThat(resultWithUniqueIp.size(), is(1));
        assertThat(resultWithUniqueIp.get(0).getApp(), equalTo("ewm-main-service"));
        assertThat(resultWithUniqueIp.get(0).getUri(), equalTo("/events/1"));
        assertThat(resultWithUniqueIp.get(0).getHits(), is(2));

        //Вывод статистики при ограниченном диапазоне timestamp

        List<ViewStats> resultWithRange =
                statsService.get("2023-01-06 11:00:23", "2023-04-06 11:00:23",
                        new String[]{"/events", "/events/1"}, false);

        assertThat(resultWithRange, notNullValue());
        assertThat(resultWithRange.size(), is(2));
        assertThat(resultWithRange.get(0).getApp(), equalTo("ewm-main-service"));
        assertThat(resultWithRange.get(0).getUri(), equalTo("/events"));
        assertThat(resultWithRange.get(0).getHits(), is(1));
        assertThat(resultWithRange.get(1).getApp(), equalTo("ewm-main-service"));
        assertThat(resultWithRange.get(1).getUri(), equalTo("/events/1"));
        assertThat(resultWithRange.get(1).getHits(), is(1));
    }
}
