package com.sprachreise.api.controller;

import com.sprachreise.api.entity.Course;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerApplication;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.CourseRepository;
import com.sprachreise.api.repository.TrainerApplicationRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.LoggingMailService;
import com.sprachreise.api.service.PdfGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Map<String, Long> LEVEL_IDS = Map.of(
        "A1", 1L, "A2", 2L, "B1", 3L, "B2", 4L, "C1", 5L, "C2", 6L
    );

    private final CourseRepository courseRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final TrainerApplicationRepository applicationRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final UserRepository userRepository;
    private final LoggingMailService mailService;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminController(CourseRepository courseRepository,
                           PdfGeneratorService pdfGeneratorService,
                           TrainerApplicationRepository applicationRepository,
                           TrainerProfileRepository trainerProfileRepository,
                           UserRepository userRepository,
                           LoggingMailService mailService) {
        this.courseRepository         = courseRepository;
        this.pdfGeneratorService      = pdfGeneratorService;
        this.applicationRepository    = applicationRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.userRepository           = userRepository;
        this.mailService              = mailService;
    }

    // ── Candidatures formateurs ───────────────────────────────────────────────

    @GetMapping("/applications")
    public ResponseEntity<?> listApplications(
            @RequestParam(defaultValue = "PENDING") String status) {
        TrainerApplication.Status s = TrainerApplication.Status.valueOf(status.toUpperCase());
        return ResponseEntity.ok(applicationRepository.findAllByStatus(s));
    }

    @PostMapping("/applications/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TrainerApplication app = applicationRepository.findById(id).orElse(null);
        if (app == null) return ResponseEntity.notFound().build();

        String levelCode = body.getOrDefault("assignedLevelCode", "A1").toString();
        int maxStudents  = Integer.parseInt(body.getOrDefault("maxStudents", "30").toString());
        String tempPass  = "Trainer@" + (int)(Math.random() * 9000 + 1000);

        User user;
        Optional<User> existing = userRepository.findByEmail(app.getEmail());
        if (existing.isPresent()) {
            user = existing.get();
        } else {
            user = new User();
            user.setEmail(app.getEmail());
            user.setPasswordHash(new BCryptPasswordEncoder().encode(tempPass));
            user.setFirstName(app.getFirstName() != null ? app.getFirstName() : "");
            user.setLastName(app.getLastName()   != null ? app.getLastName()   : "");
            user.setPhone(app.getPhone());
            user.setBio(app.getBio());
        }
        user.setRole(Role.TRAINER);
        user.setActive(true);
        userRepository.save(user);

        TrainerProfile profile = trainerProfileRepository.findByUserId(user.getId())
                .orElse(new TrainerProfile());
        profile.setUser(user);
        profile.setAssignedLevelId(LEVEL_IDS.getOrDefault(levelCode, 1L));
        profile.setMaxStudents(maxStudents);
        trainerProfileRepository.save(profile);

        app.setStatus(TrainerApplication.Status.APPROVED);
        app.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(app);

        mailService.sendWelcomeTrainer(user.getEmail(), tempPass);

        return ResponseEntity.ok(Map.of(
            "message", "Candidature approuvée",
            "trainerEmail", user.getEmail(),
            "assignedLevel", levelCode
        ));
    }

    @PostMapping("/applications/{id}/reject")
    @Transactional
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        TrainerApplication app = applicationRepository.findById(id).orElse(null);
        if (app == null) return ResponseEntity.notFound().build();

        String motif = body.getOrDefault("motif", "Dossier incomplet").toString();
        app.setStatus(TrainerApplication.Status.REJECTED);
        app.setReviewMotif(motif);
        app.setReviewedAt(LocalDateTime.now());
        applicationRepository.save(app);

        mailService.sendRejection(app.getEmail(), motif);
        return ResponseEntity.ok(Map.of("message", "Candidature refusée"));
    }

    // ── Génération PDFs cours ─────────────────────────────────────────────────

    @PostMapping("/generate-course-pdfs")
    @Transactional
    public ResponseEntity<?> generateAllCoursePdfs() {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, failed = 0;

        for (Course course : courseRepository.findAll()) {
            try {
                String pdfPath = pdfGeneratorService.generateCoursePdf(course);
                entityManager.createNativeQuery(
                    "UPDATE courses SET pdf_path = ? WHERE id = ?")
                    .setParameter(1, pdfPath)
                    .setParameter(2, course.getId())
                    .executeUpdate();
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("courseId", course.getId());
                r.put("title", course.getTitle());
                r.put("pdfPath", pdfPath);
                r.put("status", "OK");
                results.add(r);
                success++;
            } catch (Exception e) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("courseId", course.getId());
                r.put("status", "ERROR");
                r.put("error", e.getMessage());
                results.add(r);
                failed++;
            }
        }
        return ResponseEntity.ok(Map.of(
            "total", success + failed, "success", success, "failed", failed, "results", results
        ));
    }
}
