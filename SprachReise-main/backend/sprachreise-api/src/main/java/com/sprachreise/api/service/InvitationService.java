package com.sprachreise.api.service;

import com.sprachreise.api.entity.TrainerInvitation;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.TrainerInvitationRepository;
import com.sprachreise.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

    private final TrainerInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final LoggingMailService mailService;

    public InvitationService(TrainerInvitationRepository invitationRepository,
                             UserRepository userRepository,
                             LoggingMailService mailService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
    }

    @Async("invitationExecutor")
    public void sendInvitations(List<String> emails, String adminEmail, String template) {
        Long adminId = resolveAdminId(adminEmail);
        if (adminId == null) {
            log.warn("InvitationService.sendInvitations: admin not found for email='{}', falling back to id=1", adminEmail);
            adminId = 1L;
        }
        String tpl = (template == null || template.isBlank()) ? "FR" : template.toUpperCase();

        for (String email : emails) {
            if (email == null) continue;
            String clean = email.trim().toLowerCase();
            if (clean.isEmpty()) continue;
            try {
                String token = UUID.randomUUID().toString();
                TrainerInvitation invitation = new TrainerInvitation();
                invitation.setEmail(clean);
                invitation.setToken(token);
                invitation.setSentBy(adminId);
                invitation.setStatus(TrainerInvitation.Status.SENT);
                invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
                invitationRepository.save(invitation);

                mailService.sendInvitation(clean, token, tpl);
            } catch (Exception ex) {
                log.error("Failed to send invitation to {}: {}", clean, ex.getMessage());
            }
        }
    }

    private Long resolveAdminId(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) return null;
        return userRepository.findByEmail(adminEmail.trim().toLowerCase())
            .map(User::getId)
            .orElse(null);
    }
}
