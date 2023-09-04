package ru.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ViewStats;
import ru.practicum.model.Stats;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsStorage extends JpaRepository<Stats, Integer> {

    @Query("select new ru.practicum.ViewStats(s.app, s.uri, count(s.ip))" +
            " from Stats as s" +
            " where s.timestamp >= :start" +
            " and s.timestamp <= :end" +
            " group by s.app, s.uri" +
            " order by count(s.ip) desc")
    List<ViewStats> findStatsAll(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    @Query("select new ru.practicum.ViewStats(s.app, s.uri, count(distinct s.ip))" +
            " from Stats as s" +
            " where s.timestamp >= :start" +
            " and s.timestamp <= :end" +
            " group by s.app, s.uri" +
            " order by count(s.ip) desc")
    List<ViewStats> findStatsAllWithUniqueIp(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);


    @Query("select new ru.practicum.ViewStats(s.app, s.uri, count(distinct s.ip))" +
            " from Stats as s" +
            " where s.uri in :uris" +
            " and s.timestamp >= :start" +
            " and s.timestamp <= :end" +
            " group by s.app, s.uri" +
            " order by count(s.ip) desc")
    List<ViewStats> findStatsForUrisWithUniqueIp(@Param("uris") String[] uris,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    @Query("select new ru.practicum.ViewStats(s.app, s.uri, count(s.ip))" +
            " from Stats as s" +
            " where s.uri in :uris" +
            " and s.timestamp >= :start" +
            " and s.timestamp <= :end" +
            " group by s.app, s.uri" +
            " order by count(s.ip) desc")
    List<ViewStats> findStatsForUris(@Param("uris") String[] uris,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

}
