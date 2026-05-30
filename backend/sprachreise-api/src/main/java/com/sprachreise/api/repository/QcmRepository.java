package com.sprachreise.api.repository;

import com.sprachreise.api.entity.Qcm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QcmRepository extends JpaRepository<Qcm, Long> {
    List<Qcm> findByLevelIdOrderByIdAsc(Long levelId);
}
