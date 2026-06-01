package com.sprachreise.api.repository;

import com.sprachreise.api.entity.TrainerApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerApplicationRepository extends JpaRepository<TrainerApplication, Long> {

    List<TrainerApplication> findAllByStatus(TrainerApplication.Status status);

    Optional<TrainerApplication> findByIdAndStatus(Long id, TrainerApplication.Status status);

    Optional<TrainerApplication> findByEmail(String email);
}
