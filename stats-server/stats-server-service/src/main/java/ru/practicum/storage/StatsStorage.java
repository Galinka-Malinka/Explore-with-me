package ru.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.Stats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsStorage extends JpaRepository<Stats, Integer> {

    List<String> findDistinctAppByUri(String uri);

    Integer countDistinctIpByAppAndUriAndTimestampGreaterThanAndTimestampLessThan(String app, String uri,
                                                                                  LocalDateTime start,
                                                                                  LocalDateTime end);

    Integer countIpByAppAndUriAndTimestampGreaterThanAndTimestampLessThan(String app, String uri,
                                                                          LocalDateTime start, LocalDateTime end);

    @Query("select distinct s.app" +
            " from Stats as s")
    List<String> findDistinctApp();

    List<String> findUriByApp(String app);

}
