package com.sprachreise.api.repository;

import com.sprachreise.api.entity.SessionAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionAttendeeRepository extends JpaRepository<SessionAttendee, Long> {
    Optional<SessionAttendee> findBySessionIdAndLearnerId(Long sessionId, Long learnerId);
    List<SessionAttendee> findBySessionId(Long sessionId);
    List<SessionAttendee> findByLearnerId(Long learnerId);
}
