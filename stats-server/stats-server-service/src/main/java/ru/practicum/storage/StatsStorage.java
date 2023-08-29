package ru.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Stats;

import java.util.List;

public interface StatsStorage extends JpaRepository<Stats, Integer> {

    List<String> findDistinctAppByUri(String uri);

    Integer countDistinctIpByAppAndUri(String app, String uri);

    Integer countIpByAppAndUri(String app, String uri);

}
