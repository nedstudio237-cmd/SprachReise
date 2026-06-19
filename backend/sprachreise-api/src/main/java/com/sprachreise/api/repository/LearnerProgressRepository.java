package com.sprachreise.api.repository;

import com.sprachreise.api.entity.LearnerProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnerProgressRepository extends JpaRepository<LearnerProgress, Long> {

    Optional<LearnerProgress> findByLearnerIdAndLevelId(Long learnerId, Long levelId);
    List<LearnerProgress> findAllByLearnerId(Long learnerId);
    List<LearnerProgress> findAllByLevelIdOrderByCompletionPercentageDesc(Long levelId);
    long countByCertifiedTrue();
}
