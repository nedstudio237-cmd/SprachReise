package com.sprachreise.api.repository;

import com.sprachreise.api.entity.ExamSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamSubmissionRepository extends JpaRepository<ExamSubmission, Long> {
    List<ExamSubmission> findByExamIdOrderBySubmittedAtDesc(Long examId);
}
