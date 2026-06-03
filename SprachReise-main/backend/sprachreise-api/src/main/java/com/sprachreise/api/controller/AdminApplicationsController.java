package com.sprachreise.api.controller;

import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerApplication;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.TrainerApplicationRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.LoggingMailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/applications")
public class AdminApplicationsController {

    private static final Map<String, Long> LEVEL_IDS = Map.of(
        "A1", 1L, "A2", 2L, "B1", 3L, "B2", 4L, "C1", 5L, "C2", 6L
    );
    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L, "A1", 2L, "A2", 3L, "B1", 4L, "B2", 5L, "C1", 6L, "C2"
    );

    private final TrainerApplicationRepository applicationRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final UserRepository userRepository;
    private final LoggingMailService mailService;
    private final PasswordEncoder passwordEncoder;

    public AdminApplicationsController(TrainerApplicationRepository applicationRepository,
                                       TrainerProfileRepository trainerProfileRepository,
                                       UserRepository userRepository,
                                       LoggingMailService mailService,
                                       PasswordEncoder passwordEncoder) {
        this.applicationRepository = applicationRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(value = "status", defaultValue = "PENDING") String status) {
        TrainerApplication.Status s;
        try {
            s = TrainerApplication.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Statut invalide"));
        }
        List<Map<String, Object>> rows = applicationRepository.findAllByStatus(s).stream()
            .map(AdminApplicationsController::toMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TrainerApplication app = applicationRepository.findById(id).orElse(null);
        if (app == null) return ResponseEntity.notFound().build();
        if (app.getStatus() != TrainerApplication.Status.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Candidature déjà traitée"));
        }

        String levelCode = body.get("assignedLevelCode") == null ? "" : body.get("assignedLevelCode").toString().toUpperCase();
        Long levelId = LEVEL_IDS.get(levelCode);
        if (levelId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Niveau assigné invalide (A1..C2)"));
        }

        int maxStudents = 30;
        Object ms = body.get("maxStudents");
        if (ms != null) {
            try {
                maxStudents = Integer.parseInt(ms.toString());
            } catch (NumberFormatException ex) {
                return ResponseEntity.badRequest().body(Map.of("error", "maxStudents invalide"));
            }
            if (maxStudents <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "maxStudents doit être > 0"));
            }
        }

        // Find or create user
        User user = app.getUser();
        String tempPassword = null;
        if (user == null) {
            user = userRepository.findByEmail(app.getEmail()).orElse(null);
        }
        if (user == null) {
            tempPassword = generateTempPassword();
            user = new User();
            user.setEmail(app.getEmail());
            user.setPasswordHash(passwordEncoder.encode(tempPassword));
            user.setFirstName(app.getFirstName() == null ? "" : app.getFirstName());
            user.setLastName(app.getLastName() == null ? "" : app.getLastName());
            user.setPhone(app.getPhone());
            user.setBio(app.getBio());
            user.setRole(Role.TRAINER);
            user.setEmailVerified(true);
            user.setActive(true);
            user = userRepository.save(user);
        } else {
            // Elevate role to TRAINER
            user.setRole(Role.TRAINER);
            user.setActive(true);
            user = userRepository.save(user);
        }

        // Create or update trainer profile
        TrainerProfile profile = trainerProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile == null) {
            profile = new TrainerProfile();
            profile.setUser(user);
        }
        profile.setAssignedLevelId(levelId);
        profile.setMaxStudents(maxStudents);
        if (profile.getCurrentStudents() == null) profile.setCurrentStudents(0);
        profile = trainerProfileRepository.save(profile);

        app.setStatus(TrainerApplication.Status.APPROVED);
        app.setReviewedAt(LocalDateTime.now());
        app.setUser(user);
        applicationRepository.save(app);

        if (tempPassword != null) {
            mailService.sendWelcomeTrainer(user.getEmail(), tempPassword);
        } else {
            mailService.sendWelcomeTrainer(user.getEmail(), "(votre mot de passe existant)");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", profile.getId());
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("assignedLevelId", profile.getAssignedLevelId());
        response.put("assignedLevelCode", LEVEL_CODES.getOrDefault(profile.getAssignedLevelId(), "?"));
        response.put("maxStudents", profile.getMaxStudents());
        response.put("currentStudents", profile.getCurrentStudents());
        response.put("applicationStatus", app.getStatus().name());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TrainerApplication app = applicationRepository.findById(id).orElse(null);
        if (app == null) return ResponseEntity.notFound().build();
        if (app.getStatus() != TrainerApplication.Status.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Candidature déjà traitée"));
        }

        String motif = body.get("motif") == null ? "" : body.get("motif").toString().trim();
        if (motif.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Motif requis"));
        }

        app.setStatus(TrainerApplication.Status.REJECTED);
        app.setReviewMotif(motif);
        app.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(app);

        mailService.sendRejection(app.getEmail(), motif);

        return ResponseEntity.ok(Map.of(
            "id", app.getId(),
            "status", app.getStatus().name(),
            "reviewMotif", motif
        ));
    }

    private static String generateTempPassword() {
        return "Sr" + UUID.randomUUID().toString().substring(0, 10) + "!";
    }

    private static Map<String, Object> toMap(TrainerApplication app) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", app.getId());
        m.put("email", app.getEmail());
        m.put("firstName", app.getFirstName());
        m.put("lastName", app.getLastName());
        m.put("phone", app.getPhone());
        m.put("bio", app.getBio());
        m.put("nativeLanguage", app.getNativeLanguage());
        m.put("requestedLevelId", app.getRequestedLevelId());
        m.put("requestedLevelCode", LEVEL_CODES.getOrDefault(app.getRequestedLevelId(), "?"));
        m.put("status", app.getStatus().name());
        m.put("diplomaPdfPath", app.getDiplomaPdfPath());
        m.put("motivation", app.getMotivation());
        m.put("reviewedBy", app.getReviewedBy());
        m.put("reviewMotif", app.getReviewMotif());
        m.put("submittedAt", app.getSubmittedAt());
        m.put("reviewedAt", app.getReviewedAt());
        return m;
    }
}
