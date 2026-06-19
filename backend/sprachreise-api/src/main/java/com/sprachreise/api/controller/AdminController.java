package com.sprachreise.api.controller;

import com.sprachreise.api.entity.Course;
import com.sprachreise.api.entity.LearnerProgress;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.CourseRepository;
import com.sprachreise.api.repository.LearnerProgressRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.LoggingMailService;
import com.sprachreise.api.service.PdfGeneratorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Map<String, Long> LEVEL_IDS = Map.of(
        "A1",1L,"A2",2L,"B1",3L,"B2",4L,"C1",5L,"C2",6L
    );
    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L,"A1",2L,"A2",3L,"B1",4L,"B2",5L,"C1",6L,"C2"
    );

    @Value("${storage.upload-dir}")
    private String storageDir;

    private final CourseRepository          courseRepository;
    private final TrainerProfileRepository  trainerProfileRepository;
    private final UserRepository            userRepository;
    private final LearnerProgressRepository progressRepository;
    private final LoggingMailService        mailService;
    private final PdfGeneratorService       pdfGeneratorService;
    private final PasswordEncoder           passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminController(CourseRepository courseRepository,
                           TrainerProfileRepository trainerProfileRepository,
                           UserRepository userRepository,
                           LearnerProgressRepository progressRepository,
                           LoggingMailService mailService,
                           PdfGeneratorService pdfGeneratorService,
                           PasswordEncoder passwordEncoder) {
        this.courseRepository         = courseRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.userRepository           = userRepository;
        this.progressRepository       = progressRepository;
        this.mailService              = mailService;
        this.pdfGeneratorService      = pdfGeneratorService;
        this.passwordEncoder          = passwordEncoder;
    }

    // ── Dashboard enrichi ─────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard() {
        long totalLearners     = userRepository.countByRole(Role.LEARNER);
        long activeLearners    = userRepository.countByRoleAndActiveTrue(Role.LEARNER);
        long totalTrainers     = userRepository.countByRole(Role.TRAINER);
        long approvedTrainers  = trainerProfileRepository.findByStatus(TrainerProfile.Status.APPROVED).size();
        long pendingApps       = trainerProfileRepository.findByStatus(TrainerProfile.Status.PENDING).size();
        long totalCourses      = courseRepository.count();
        long publishedCourses  = courseRepository.findAllPublished().size();
        long totalCertificates = progressRepository.countByCertifiedTrue();

        // Candidatures en attente depuis > 48h
        long overdueApps = trainerProfileRepository.findByStatus(TrainerProfile.Status.PENDING).stream()
            .filter(p -> p.getSubmittedAt() != null &&
                ChronoUnit.HOURS.between(p.getSubmittedAt(), LocalDateTime.now()) > 48)
            .count();

        // Score QCM moyen global
        double avgQcmScore = progressRepository.findAll().stream()
            .filter(p -> p.getQcmAvgScore() != null && p.getQcmAvgScore().doubleValue() > 0)
            .mapToDouble(p -> p.getQcmAvgScore().doubleValue())
            .average().orElse(0.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLearners",      totalLearners);
        result.put("activeLearners",     activeLearners);
        result.put("totalTrainers",      totalTrainers);
        result.put("approvedTrainers",   approvedTrainers);
        result.put("pendingApplications",pendingApps);
        result.put("overdueApplications",overdueApps);
        result.put("totalCourses",       totalCourses);
        result.put("publishedCourses",   publishedCourses);
        result.put("totalCertificates",  totalCertificates);
        result.put("avgQcmScore",        Math.round(avgQcmScore * 10.0) / 10.0);

        // Répartition par niveau
        Map<String, Long> levelDistribution = new LinkedHashMap<>();
        for (var entry : LEVEL_IDS.entrySet()) {
            long count = progressRepository.findAllByLevelIdOrderByCompletionPercentageDesc(entry.getValue()).size();
            levelDistribution.put(entry.getKey(), count);
        }
        result.put("learnersByLevel", levelDistribution);

        return ResponseEntity.ok(result);
    }

    // ── Détail utilisateur ────────────────────────────────────────────────────
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserDetail(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id",        user.getId());
        result.put("email",     user.getEmail());
        result.put("firstName", user.getFirstName());
        result.put("lastName",  user.getLastName());
        result.put("role",      user.getRole().name());
        result.put("active",    user.getActive());
        result.put("phone",     user.getPhone());
        result.put("city",      user.getCity());
        result.put("bio",       user.getBio());
        result.put("photoUrl",  user.getPhotoUrl());
        result.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

        if (user.getRole() == Role.LEARNER) {
            List<LearnerProgress> progresses = progressRepository.findAllByLearnerId(userId);
            List<Map<String, Object>> progressList = new ArrayList<>();
            for (LearnerProgress p : progresses) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("levelId",             p.getLevelId());
                pm.put("levelCode",           LEVEL_CODES.getOrDefault(p.getLevelId(), "?"));
                pm.put("completionPercentage",p.getCompletionPercentage());
                pm.put("coursesCompleted",    p.getCoursesCompleted());
                pm.put("sessionsAttended",    p.getSessionsAttended());
                pm.put("qcmAvgScore",         p.getQcmAvgScore());
                pm.put("totalMinutes",        p.getTotalMinutes());
                pm.put("certified",           p.getCertified());
                pm.put("certifiedAt",         p.getCertifiedAt() != null ? p.getCertifiedAt().toString() : null);
                progressList.add(pm);
            }
            result.put("progress", progressList);
        }

        if (user.getRole() == Role.TRAINER) {
            trainerProfileRepository.findByUserId(userId).ifPresent(p -> {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("profileId",      p.getId());
                pm.put("teachingLevel",  p.getTeachingLevelCode());
                pm.put("teachingLang",   p.getTeachingLanguageCode());
                pm.put("maxStudents",    p.getMaxStudents());
                pm.put("ratingAvg",      p.getRatingAvg());
                pm.put("status",         p.getStatus().name());
                pm.put("motivation",     p.getMotivation());
                pm.put("submittedAt",    p.getSubmittedAt() != null ? p.getSubmittedAt().toString() : null);
                List<Course> courses = courseRepository.findAllByTrainerId(userId);
                pm.put("totalCourses",   courses.size());
                pm.put("publishedCourses", courses.stream().filter(c -> c.getStatus() == Course.CourseStatus.PUBLISHED).count());
                result.put("trainerProfile", pm);
            });
        }

        return ResponseEntity.ok(result);
    }

    // ── Reset mot de passe ────────────────────────────────────────────────────
    @PostMapping("/users/{userId}/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        String tempPassword = "Sr" + (100000 + (int)(Math.random() * 900000)) + "!";
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        mailService.sendPasswordReset(user.getEmail(), user.getFirstName(), tempPassword);

        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé et envoyé par email"));
    }

    // ── Modifier quota formateur ───────────────────────────────────────────────
    @PostMapping("/users/{userId}/trainer-quota")
    @Transactional
    public ResponseEntity<?> updateTrainerQuota(@PathVariable Long userId,
                                                 @RequestBody Map<String, Object> body) {
        TrainerProfile profile = trainerProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return ResponseEntity.badRequest().body(Map.of("error", "Profil formateur introuvable"));

        Object q = body.get("maxStudents");
        if (!(q instanceof Number)) return ResponseEntity.badRequest().body(Map.of("error", "maxStudents requis"));
        profile.setMaxStudents(((Number) q).intValue());
        trainerProfileRepository.save(profile);

        return ResponseEntity.ok(Map.of("maxStudents", profile.getMaxStudents()));
    }

    // ── Réassigner niveau formateur ───────────────────────────────────────────
    @PostMapping("/users/{userId}/trainer-reassign")
    @Transactional
    public ResponseEntity<?> reassignTrainerLevel(@PathVariable Long userId,
                                                   @RequestBody Map<String, String> body) {
        String newLevel = body == null ? null : body.get("level");
        if (newLevel == null || !LEVEL_IDS.containsKey(newLevel))
            return ResponseEntity.badRequest().body(Map.of("error", "Niveau invalide (A1–C2)"));

        TrainerProfile profile = trainerProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return ResponseEntity.badRequest().body(Map.of("error", "Profil formateur introuvable"));

        String oldLevel = profile.getTeachingLevelCode();
        profile.setTeachingLevelCode(newLevel);
        trainerProfileRepository.save(profile);

        return ResponseEntity.ok(Map.of("oldLevel", oldLevel, "newLevel", newLevel));
    }

    // ── Suspendre / réactiver ─────────────────────────────────────────────────
    @PostMapping("/users/{userId}/toggle-active")
    @Transactional
    public ResponseEntity<?> toggleActive(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        user.setActive(!user.getActive());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("active", user.getActive()));
    }

    // ── Liste utilisateurs ────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestParam(defaultValue = "ALL") String role) {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            if (!"ALL".equals(role) && !u.getRole().name().equals(role)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        u.getId());
            m.put("email",     u.getEmail());
            m.put("firstName", u.getFirstName());
            m.put("lastName",  u.getLastName());
            m.put("role",      u.getRole().name());
            m.put("active",    u.getActive());
            m.put("phone",     u.getPhone());
            m.put("city",      u.getCity());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ── Liste candidatures ────────────────────────────────────────────────────
    @GetMapping("/applications")
    public ResponseEntity<?> listApplications(@RequestParam(defaultValue = "ALL") String status) {
        List<TrainerProfile> profiles;
        if ("ALL".equals(status)) {
            profiles = trainerProfileRepository.findAll();
        } else {
            try { profiles = trainerProfileRepository.findByStatus(TrainerProfile.Status.valueOf(status)); }
            catch (IllegalArgumentException e) { profiles = trainerProfileRepository.findAll(); }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainerProfile p : profiles) {
            userRepository.findById(p.getUserId()).ifPresent(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("profileId",     p.getId());
                m.put("userId",        u.getId());
                m.put("firstName",     u.getFirstName());
                m.put("lastName",      u.getLastName());
                m.put("email",         u.getEmail());
                m.put("phone",         u.getPhone());
                m.put("bio",           u.getBio());
                m.put("nativeLanguage",p.getNativeLanguage());
                m.put("teachingLevel", p.getTeachingLevelCode());
                m.put("teachingLang",  p.getTeachingLanguageCode());
                m.put("motivation",    p.getMotivation());
                m.put("diplomaPath",   p.getDiplomaPdfPath());
                m.put("status",        p.getStatus().name());
                m.put("reviewMotif",   p.getReviewMotif());
                m.put("submittedAt",   p.getSubmittedAt() != null ? p.getSubmittedAt().toString() : null);
                m.put("reviewedAt",    p.getReviewedAt() != null ? p.getReviewedAt().toString() : null);
                result.add(m);
            });
        }
        result.sort((a, b) -> {
            String da = (String) a.get("submittedAt"), db = (String) b.get("submittedAt");
            if (da == null) return 1; if (db == null) return -1; return db.compareTo(da);
        });
        return ResponseEntity.ok(result);
    }

    // ── Approuver candidature ─────────────────────────────────────────────────
    @PostMapping("/applications/{profileId}/approve")
    @Transactional
    public ResponseEntity<?> approveApplication(@PathVariable Long profileId,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        TrainerProfile profile = trainerProfileRepository.findById(profileId).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        Integer maxStudents = 30;
        if (body != null && body.get("maxStudents") instanceof Number n) maxStudents = n.intValue();

        profile.setStatus(TrainerProfile.Status.APPROVED);
        profile.setReviewedAt(LocalDateTime.now());
        profile.setMaxStudents(maxStudents);
        trainerProfileRepository.save(profile);

        userRepository.findById(profile.getUserId()).ifPresent(u ->
            mailService.sendTrainerApproval(u.getEmail(), u.getFirstName()));

        return ResponseEntity.ok(Map.of("message", "Candidature approuvée", "profileId", profileId));
    }

    // ── Refuser candidature ───────────────────────────────────────────────────
    @PostMapping("/applications/{profileId}/reject")
    @Transactional
    public ResponseEntity<?> rejectApplication(@PathVariable Long profileId,
                                                @RequestBody Map<String, String> body) {
        String motif = body == null ? "" : body.getOrDefault("motif", "").trim();
        if (motif.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Le motif est obligatoire"));

        TrainerProfile profile = trainerProfileRepository.findById(profileId).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();

        profile.setStatus(TrainerProfile.Status.REJECTED);
        profile.setReviewMotif(motif);
        profile.setReviewedAt(LocalDateTime.now());
        trainerProfileRepository.save(profile);

        userRepository.findById(profile.getUserId()).ifPresent(u ->
            mailService.sendTrainerRejection(u.getEmail(), u.getFirstName(), motif));

        return ResponseEntity.ok(Map.of("message", "Candidature refusée", "profileId", profileId));
    }

    // ── Consulter le diplôme ──────────────────────────────────────────────────
    @GetMapping("/applications/{profileId}/diploma")
    public ResponseEntity<Resource> getDiploma(@PathVariable Long profileId) {
        TrainerProfile profile = trainerProfileRepository.findById(profileId).orElse(null);
        if (profile == null || profile.getDiplomaPdfPath() == null) return ResponseEntity.notFound().build();

        File file = Paths.get(storageDir).resolve(profile.getDiplomaPdfPath()).normalize().toFile();
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"diploma.pdf\"")
                .body(resource);
    }

    // ── Modération cours ──────────────────────────────────────────────────────
    @GetMapping("/courses")
    public ResponseEntity<?> listPublishedCourses() {
        List<Course> courses = courseRepository.findAllPublished();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          c.getId());
            m.put("title",       c.getTitle());
            m.put("description", c.getDescription());
            m.put("theme",       c.getTheme());
            m.put("levelId",     c.getLevelId());
            m.put("status",      c.getStatus().name());
            m.put("viewCount",   c.getViewCount() != null ? c.getViewCount() : 0);
            m.put("trainerName", c.getTrainer() != null
                ? c.getTrainer().getFirstName() + " " + c.getTrainer().getLastName() : "?");
            m.put("trainerId",   c.getTrainer() != null ? c.getTrainer().getId() : null);
            m.put("createdAt",   c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/courses/{courseId}/remove")
    @Transactional
    public ResponseEntity<?> removeCourse(@PathVariable Long courseId,
                                           @RequestBody(required = false) Map<String, String> body) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return ResponseEntity.notFound().build();
        course.setStatus(Course.CourseStatus.REMOVED);
        courseRepository.save(course);
        return ResponseEntity.ok(Map.of("message", "Cours retiré", "courseId", courseId));
    }

    // ── Génération PDFs ───────────────────────────────────────────────────────
    @PostMapping("/generate-course-pdfs")
    @Transactional
    public ResponseEntity<?> generateAllCoursePdfs() {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, failed = 0;
        for (var course : courseRepository.findAll()) {
            try {
                String pdfPath = pdfGeneratorService.generateCoursePdf(course);
                entityManager.createNativeQuery("UPDATE courses SET pdf_path = ? WHERE id = ?")
                        .setParameter(1, pdfPath).setParameter(2, course.getId()).executeUpdate();
                results.add(Map.of("courseId", course.getId(), "status", "OK", "pdfPath", pdfPath));
                success++;
            } catch (Exception e) {
                results.add(Map.of("courseId", course.getId(), "status", "ERROR", "error", e.getMessage()));
                failed++;
            }
        }
        return ResponseEntity.ok(Map.of("total", success + failed, "success", success, "failed", failed, "results", results));
    }
}
