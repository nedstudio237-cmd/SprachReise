package com.sprachreise.api.repository;

import com.sprachreise.api.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
           "(m.senderId = :a AND m.recipientId = :b) OR " +
           "(m.senderId = :b AND m.recipientId = :a) " +
           "ORDER BY m.sentAt ASC")
    List<Message> findByPair(@Param("a") Long a, @Param("b") Long b);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :now " +
           "WHERE m.recipientId = :me AND m.senderId = :other AND m.readAt IS NULL")
    int markAsRead(@Param("me") Long me, @Param("other") Long other, @Param("now") LocalDateTime now);

    @Query("SELECT m FROM Message m WHERE m.senderId = :userId OR m.recipientId = :userId ORDER BY m.sentAt DESC")
    List<Message> findAllForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipientId = :me AND m.senderId = :other AND m.readAt IS NULL")
    long countUnread(@Param("me") Long me, @Param("other") Long other);
}
