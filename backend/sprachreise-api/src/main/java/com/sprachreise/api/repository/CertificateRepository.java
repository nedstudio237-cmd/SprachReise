package com.sprachreise.api.repository;

import com.sprachreise.api.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findAllByLearnerIdOrderByIssuedAtDesc(Long learnerId);
    Optional<Certificate> findByLearnerIdAndLevelCode(Long learnerId, String levelCode);
    boolean existsByLearnerIdAndLevelCodeAndRevokedFalse(Long learnerId, String levelCode);
    List<Certificate> findAllByRevokedFalseOrderByIssuedAtDesc();
}
