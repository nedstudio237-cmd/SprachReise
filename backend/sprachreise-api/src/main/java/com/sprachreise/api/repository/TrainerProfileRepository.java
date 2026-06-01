package com.sprachreise.api.repository;

import com.sprachreise.api.entity.TrainerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, Long> {

    @Query("SELECT tp FROM TrainerProfile tp JOIN FETCH tp.user u WHERE u.active = true AND u.role = com.sprachreise.api.entity.Role.TRAINER ORDER BY tp.ratingAvg DESC")
    List<TrainerProfile> findAllActive();

    @Query("SELECT tp FROM TrainerProfile tp JOIN FETCH tp.user u WHERE tp.user.id = :userId")
    Optional<TrainerProfile> findByUserId(Long userId);
}
