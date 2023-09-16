package ru.practicum.comment.storage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.comment.dto.FullCommentDto;
import ru.practicum.comment.dto.ShortCommentDto;
import ru.practicum.comment.model.Comment;

import java.util.List;

public interface CommentStorage extends JpaRepository<Comment, Integer>, QuerydslPredicateExecutor<Comment> {

    @Query("select new ru.practicum.comment.dto.ShortCommentDto(" +
            " u.name," +
            " c.text," +
            " c.created)" +
            " from Comment as c" +
            " join c.author as u" +
            " join c.event as e" +
            " where e.id = ?1")
    List<ShortCommentDto> findByEventId(Integer eventId);

    Boolean existsByEventId(Integer eventId);

    @Query("select new ru.practicum.comment.dto.FullCommentDto(" +
            " c.id," +
            " e.title," +
            " c.text," +
            " u," +
            " c.created)" +
            " from Comment as c" +
            " join c.event as e" +
            " join c.author as u")
    List<FullCommentDto> findFullCommentsByEventId(Integer eventId, Pageable pageable);

    void deleteAllByIdIn(Integer[] ids);

    void deleteAllByEventIdIn(Integer[] events);
}
