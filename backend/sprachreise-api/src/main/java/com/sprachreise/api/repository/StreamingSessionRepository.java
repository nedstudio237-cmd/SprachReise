package com.sprachreise.api.repository;

import com.sprachreise.api.entity.StreamingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StreamingSessionRepository extends JpaRepository<StreamingSession, Long> {

    @Query("SELECT s FROM StreamingSession s WHERE s.status IN ('SCHEDULED','LIVE') ORDER BY s.scheduledStart ASC")
    List<StreamingSession> findUpcomingAndLive();

    @Query("SELECT s FROM StreamingSession s WHERE s.levelId = :levelId AND s.status IN ('SCHEDULED','LIVE') ORDER BY s.scheduledStart ASC")
    List<StreamingSession> findByLevelId(Long levelId);
}
