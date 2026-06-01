package com.sprachreise.api.controller;

import com.sprachreise.api.dto.ExamDto;
import com.sprachreise.api.dto.ExamSubmissionDto;
import com.sprachreise.api.entity.Exam;
import com.sprachreise.api.entity.ExamSubmission;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.ExamRepository;
import com.sprachreise.api.repository.ExamSubmissionRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.LoggingMailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L, "A1", 2L, "A2", 3L, "B1", 4L, "B2", 5L, "C1", 6L, "C2"
    );

    private final ExamRepository examRepository;
    private final ExamSubmissionRepository examSubmissionRepository;
    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final LoggingMailService loggingMailService;

    public ExamController(ExamRepository examRepository,
                          ExamSubmissionRepository examSubmissionRepository,
                          UserRepository userRepository,
                          TrainerProfileRepository trainerProfileRepository,
                          LoggingMailService loggingMailService) {
        this.examRepository = examRepository;
        this.examSubmissionRepository = examSubmissionRepository;
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.loggingMailService = loggingMailService;
    }

    // ----- Owner endpoints (mine first to avoid path collision) -----

    @GetMapping("/mine")
    public ResponseEntity<?> getMine() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        List<ExamDto> dtos = examRepository.findByTrainerIdOrderByIdDesc(me.getId()).stream()
            .map(e -> ExamDto.from(e, LEVEL_CODES.getOrDefault(e.getLevelId(), "?")))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> createExam(@RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<TrainerProfile> tpOpt = trainerProfileRepository.findByUserId(me.getId());
        if (tpOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Profil formateur introuvable"));
        }

        String title = trim((String) body.get("title"));
        if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Titre requis"));

        Exam exam = new Exam();
        exam.setTrainerId(me.getId());
        exam.setLevelId(tpOpt.get().getAssignedLevelId());
        exam.setTitle(title);
        exam.setInstructions(trim((String) body.get("instructions")));
        exam.setScheduledAt(parseDateTime(body.get("scheduledAt")));
        exam.setStatus(parseExamStatus(body.get("status")));

        examRepository.save(exam);
        return ResponseEntity.ok(ExamDto.from(exam, LEVEL_CODES.getOrDefault(exam.getLevelId(), "?")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExam(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Exam> opt = examRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Exam exam = opt.get();
        if (!exam.getTrainerId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas l'auteur"));
        }

        if (body.containsKey("title")) {
            String t = trim((String) body.get("title"));
            if (t.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Titre requis"));
            exam.setTitle(t);
        }
        if (body.containsKey("instructions")) exam.setInstructions(trim((String) body.get("instructions")));
        if (body.containsKey("scheduledAt")) exam.setScheduledAt(parseDateTime(body.get("scheduledAt")));
        if (body.containsKey("status")) exam.setStatus(parseExamStatus(body.get("status")));

        examRepository.save(exam);
        return ResponseEntity.ok(ExamDto.from(exam, LEVEL_CODES.getOrDefault(exam.getLevelId(), "?")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Exam> opt = examRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Exam exam = opt.get();
        if (!exam.getTrainerId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas l'auteur"));
        }
        examRepository.delete(exam);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExam(@PathVariable Long id) {
        Optional<Exam> opt = examRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Exam exam = opt.get();
        // Public when PUBLISHED; otherwise only the owner trainer can see it.
        if (exam.getStatus() != Exam.ExamStatus.PUBLISHED) {
            User me = currentUser();
            if (me == null || !exam.getTrainerId().equals(me.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Épreuve non publiée"));
            }
        }
        return ResponseEntity.ok(ExamDto.from(exam, LEVEL_CODES.getOrDefault(exam.getLevelId(), "?")));
    }

    // ----- Learner submission -----

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.LEARNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux apprenants"));
        }
        Optional<Exam> opt = examRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Exam exam = opt.get();
        if (exam.getStatus() != Exam.ExamStatus.PUBLISHED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Épreuve non publiée"));
        }

        String answerText = trim((String) body.get("answerText"));
        if (answerText.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Réponse vide"));

        ExamSubmission sub = new ExamSubmission();
        sub.setExamId(exam.getId());
        sub.setLearnerId(me.getId());
        sub.setAnswerText(answerText);
        examSubmissionRepository.save(sub);

        // Notify trainer
        userRepository.findById(exam.getTrainerId()).ifPresent(trainer -> {
            String learnerName = ((me.getFirstName() == null ? "" : me.getFirstName()) + " "
                    + (me.getLastName() == null ? "" : me.getLastName())).trim();
            if (learnerName.isEmpty()) learnerName = me.getEmail();
            loggingMailService.notifyExamSubmission(trainer.getEmail(), learnerName, exam.getTitle());
        });

        return ResponseEntity.ok(ExamSubmissionDto.from(sub, displayName(me), me.getEmail()));
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<?> listSubmissions(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Exam> opt = examRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Exam exam = opt.get();
        if (!exam.getTrainerId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas l'auteur"));
        }
        List<ExamSubmissionDto> dtos = examSubmissionRepository.findByExamIdOrderBySubmittedAtDesc(id).stream()
            .map(s -> userRepository.findById(s.getLearnerId())
                .map(u -> ExamSubmissionDto.from(s, displayName(u), u.getEmail()))
                .orElseGet(() -> ExamSubmissionDto.from(s, "Apprenant #" + s.getLearnerId(), null)))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/submissions/{subId}/grade")
    public ResponseEntity<?> grade(@PathVariable Long subId, @RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<ExamSubmission> subOpt = examSubmissionRepository.findById(subId);
        if (subOpt.isEmpty()) return ResponseEntity.notFound().build();
        ExamSubmission sub = subOpt.get();
        Optional<Exam> examOpt = examRepository.findById(sub.getExamId());
        if (examOpt.isEmpty()) return ResponseEntity.notFound().build();
        Exam exam = examOpt.get();
        if (!exam.getTrainerId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas l'auteur"));
        }

        Object rawGrade = body.get("grade");
        if (rawGrade == null) return ResponseEntity.badRequest().body(Map.of("error", "Note requise"));
        BigDecimal grade;
        try {
            grade = new BigDecimal(String.valueOf(rawGrade));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Note invalide"));
        }

        sub.setGrade(grade);
        sub.setFeedback(trim((String) body.get("feedback")));
        sub.setGradedAt(LocalDateTime.now());
        sub.setGradedBy(me.getId());
        examSubmissionRepository.save(sub);

        userRepository.findById(sub.getLearnerId()).ifPresent(learner ->
            loggingMailService.notifyExamGraded(learner.getEmail(), exam.getTitle(), grade, sub.getFeedback())
        );

        String learnerName = userRepository.findById(sub.getLearnerId())
            .map(this::displayName).orElse("Apprenant #" + sub.getLearnerId());
        String learnerEmail = userRepository.findById(sub.getLearnerId())
            .map(User::getEmail).orElse(null);
        return ResponseEntity.ok(ExamSubmissionDto.from(sub, learnerName, learnerEmail));
    }

    // ----- Helpers -----

    private String displayName(User u) {
        String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return name.isEmpty() ? u.getEmail() : name;
    }

    private Exam.ExamStatus parseExamStatus(Object raw) {
        if (raw == null) return Exam.ExamStatus.DRAFT;
        try {
            return Exam.ExamStatus.valueOf(String.valueOf(raw).toUpperCase());
        } catch (IllegalArgumentException e) {
            return Exam.ExamStatus.DRAFT;
        }
    }

    private LocalDateTime parseDateTime(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) return null;
        try {
            if (s.length() == 16) s = s + ":00";
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) return null;
        String email = ((UserDetails) principal).getUsername();
        return userRepository.findByEmail(email).orElse(null);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
