package com.sprachreise.api.controller;

import com.sprachreise.api.dto.ExamDto;
import com.sprachreise.api.dto.ExamSubmissionDto;
import com.sprachreise.api.entity.Exam;
import com.sprachreise.api.entity.ExamSubmission;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.ExamRepository;
import com.sprachreise.api.repository.ExamSubmissionRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.LoggingMailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    private final ExamRepository           examRepository;
    private final ExamSubmissionRepository examSubmissionRepository;
    private final UserRepository           userRepository;
    private final LoggingMailService       loggingMailService;

    public ExamController(ExamRepository examRepository,
                          ExamSubmissionRepository examSubmissionRepository,
                          UserRepository userRepository,
                          LoggingMailService loggingMailService) {
        this.examRepository           = examRepository;
        this.examSubmissionRepository = examSubmissionRepository;
        this.userRepository           = userRepository;
        this.loggingMailService       = loggingMailService;
    }

    @GetMapping
    public ResponseEntity<?> getPublished(@RequestParam(required = false) Long levelId) {
        List<Exam> exams = levelId != null
            ? examRepository.findByLevelIdAndStatus(levelId, Exam.ExamStatus.PUBLISHED)
            : examRepository.findByStatus(Exam.ExamStatus.PUBLISHED);
        return ResponseEntity.ok(exams.stream()
            .map(e -> ExamDto.from(e, LEVEL_CODES.getOrDefault(e.getLevelId(), "?")))
            .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExam(@PathVariable Long id) {
        Optional<Exam> opt = examRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Exam exam = opt.get();
        if (exam.getStatus() != Exam.ExamStatus.PUBLISHED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Épreuve non publiée"));
        }
        return ResponseEntity.ok(ExamDto.from(exam, LEVEL_CODES.getOrDefault(exam.getLevelId(), "?")));
    }

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

        userRepository.findById(exam.getTrainerId()).ifPresent(trainer -> {
            String name = displayName(me);
            loggingMailService.notifyExamSubmission(trainer.getEmail(), name, exam.getTitle());
        });

        return ResponseEntity.ok(ExamSubmissionDto.from(sub, displayName(me), me.getEmail()));
    }

    private String displayName(User u) {
        String n = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if (!(p instanceof UserDetails)) return null;
        return userRepository.findByEmail(((UserDetails) p).getUsername()).orElse(null);
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
