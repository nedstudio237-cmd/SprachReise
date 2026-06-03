package com.sprachreise.api.repository;

import com.sprachreise.api.entity.TrainerInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainerInvitationRepository extends JpaRepository<TrainerInvitation, Long> {

    Optional<TrainerInvitation> findByToken(String token);

    List<TrainerInvitation> findAllBySentByOrderBySentAtDesc(Long sentBy);

    List<TrainerInvitation> findAllByOrderBySentAtDesc();
}
