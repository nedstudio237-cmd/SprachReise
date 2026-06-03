package com.sprachreise.api.repository;

import com.sprachreise.api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByLearnerIdOrderByCreatedAtDesc(Long learnerId);
    Optional<Payment> findByExternalRefAndStatus(String externalRef, String status);
}
