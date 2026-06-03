package com.sprachreise.api.repository;

import com.sprachreise.api.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByTrainerIdOrderByIdDesc(Long trainerId);
}
