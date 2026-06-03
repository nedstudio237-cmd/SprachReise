package com.sprachreise.api.repository;

import com.sprachreise.api.entity.TrainerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, Long> {
    Optional<TrainerProfile> findByUserId(Long userId);
    List<TrainerProfile> findByStatus(TrainerProfile.Status status);

    @Query("SELECT p FROM TrainerProfile p WHERE p.status = 'APPROVED' AND p.teachingLevelCode = :levelCode AND (p.currentStudents < p.maxStudents) ORDER BY p.currentStudents ASC")
    List<TrainerProfile> findAvailableByLevel(String levelCode);
}
