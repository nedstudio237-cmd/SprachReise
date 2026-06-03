package com.sprachreise.api.controller;

import com.sprachreise.api.entity.TrainerApplication;
import com.sprachreise.api.entity.TrainerInvitation;
import com.sprachreise.api.repository.TrainerApplicationRepository;
import com.sprachreise.api.repository.TrainerInvitationRepository;
import com.sprachreise.api.service.LoggingMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/trainer-applications")
public class TrainerApplicationController {

    private static final Map<String, Long> LEVEL_IDS = Map.of(
        "A1", 1L, "A2", 2L, "B1", 3L, "B2", 4L, "C1", 5L, "C2", 6L
    );

    private static final long MAX_PDF_BYTES = 5L * 1024 * 1024;
    private static final int BIO_MAX_LENGTH = 1000;

    private final TrainerApplicationRepository applicationRepository;
    private final TrainerInvitationRepository invitationRepository;
    private final LoggingMailService mailService;

    @Value("${storage.upload-dir}")
    private String storageDir;

    public TrainerApplicationController(TrainerApplicationRepository applicationRepository,
                                        TrainerInvitationRepository invitationRepository,
                                        LoggingMailService mailService) {
        this.applicationRepository = applicationRepository;
        this.invitationRepository = invitationRepository;
        this.mailService = mailService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> submit(
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "bio", required = false) String bio,
            @RequestParam("requestedLevelCode") String requestedLevelCode,
            @RequestParam(value = "nativeLanguage", required = false) String nativeLanguage,
            @RequestParam(value = "motivation", required = false) String motivation,
            @RequestParam("diploma") MultipartFile diploma) {

        firstName = trim(firstName);
        lastName = trim(lastName);
        email = trim(email).toLowerCase();
        phone = trim(phone);
        bio = trim(bio);
        nativeLanguage = trim(nativeLanguage);
        motivation = trim(motivation);

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prénom, nom et email requis"));
        }
        if (!email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email invalide"));
        }
        if (bio.length() > BIO_MAX_LENGTH) {
            return ResponseEntity.badRequest().body(Map.of("error", "Biographie : " + BIO_MAX_LENGTH + " caractères max"));
        }

        Long levelId = LEVEL_IDS.get(requestedLevelCode == null ? "" : requestedLevelCode.toUpperCase());
        if (levelId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Niveau invalide (A1..C2)"));
        }

        if (diploma == null || diploma.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier diplôme requis"));
        }
        if (diploma.getSize() > MAX_PDF_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "Diplôme > 5 Mo"));
        }
        String original = diploma.getOriginalFilename() == null ? "" : diploma.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot + 1).toLowerCase();
        if (!"pdf".equals(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Format accepté : PDF"));
        }

        try {
            Path diplomasDir = Paths.get(storageDir, "diplomas");
            Files.createDirectories(diplomasDir);
            String filename = "cand_" + System.currentTimeMillis() + ".pdf";
            Path target = diplomasDir.resolve(filename);
            Files.copy(diploma.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String relative = "diplomas/" + filename;

            TrainerApplication app = new TrainerApplication();
            app.setEmail(email);
            app.setFirstName(firstName);
            app.setLastName(lastName);
            app.setPhone(phone);
            app.setBio(bio);
            app.setNativeLanguage(nativeLanguage);
            app.setRequestedLevelId(levelId);
            app.setStatus(TrainerApplication.Status.PENDING);
            app.setDiplomaPdfPath(relative);
            app.setMotivation(motivation);
            applicationRepository.save(app);

            mailService.notifyAdminNewApplication(app);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", app.getId(),
                "status", app.getStatus().name(),
                "message", "Candidature enregistrée"
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Échec de l'upload : " + e.getMessage()));
        }
    }

    @GetMapping("/from-invitation")
    public ResponseEntity<?> fromInvitation(@RequestParam("token") String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "error", "Token requis"));
        }
        TrainerInvitation invitation = invitationRepository.findByToken(token.trim()).orElse(null);
        if (invitation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("valid", false, "error", "Token introuvable"));
        }
        LocalDateTime now = LocalDateTime.now();
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(now)) {
            if (invitation.getStatus() != TrainerInvitation.Status.EXPIRED) {
                invitation.setStatus(TrainerInvitation.Status.EXPIRED);
                invitationRepository.save(invitation);
            }
            return ResponseEntity.status(HttpStatus.GONE)
                .body(Map.of("valid", false, "error", "Invitation expirée", "email", invitation.getEmail()));
        }
        if (invitation.getStatus() == TrainerInvitation.Status.SENT) {
            invitation.setStatus(TrainerInvitation.Status.OPENED);
            invitationRepository.save(invitation);
        }
        return ResponseEntity.ok(Map.of(
            "valid", true,
            "email", invitation.getEmail(),
            "status", invitation.getStatus().name(),
            "expiresAt", invitation.getExpiresAt()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStatus(@PathVariable Long id) {
        return applicationRepository.findById(id)
            .map(app -> ResponseEntity.ok((Object) Map.of(
                "id", app.getId(),
                "status", app.getStatus().name(),
                "submittedAt", app.getSubmittedAt()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
