package com.sprachreise.api.repository;

import com.sprachreise.api.entity.QcmAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QcmAttemptRepository extends JpaRepository<QcmAttempt, Long> {
    List<QcmAttempt> findByQcmIdOrderByAttemptedAtDesc(Long qcmId);
}
