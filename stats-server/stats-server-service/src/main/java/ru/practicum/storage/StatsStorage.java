package ru.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.Stats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsStorage extends JpaRepository<Stats, Integer> {

    @Query("select distinct s.app" +
            " from Stats as s" +
            " where s.uri = ?1")
    List<String> findDistinctAppByUri(String uri);

    @Query("select count(distinct s.ip)" +
            " from Stats as s" +
            " where s.app = ?1" +
            " and s.uri = ?2" +
            " and s.timestamp >= ?3" +
            " and s.timestamp <= ?4")
    Integer countDistinctIp(String app, String uri, LocalDateTime start, LocalDateTime end);

    @Query("select count(s.ip)" +
            " from Stats as s" +
            " where s.app = ?1" +
            " and s.uri = ?2" +
            " and s.timestamp >= ?3" +
            " and s.timestamp <= ?4")
    Integer countAllIp(String app, String uri, LocalDateTime start, LocalDateTime end);

    @Query("select distinct s.app" +
            " from Stats as s")
    List<String> findDistinctApp();

    @Query("select distinct s.uri" +
            " from Stats as s" +
            " where s.app = ?1")
    List<String> findDistinctUriByApp(String app);
}
