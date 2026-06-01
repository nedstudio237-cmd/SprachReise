package com.sprachreise.api.controller;

import com.sprachreise.api.dto.QcmDto;
import com.sprachreise.api.entity.Qcm;
import com.sprachreise.api.entity.QcmAttempt;
import com.sprachreise.api.entity.QcmChoice;
import com.sprachreise.api.entity.QcmQuestion;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.QcmAttemptRepository;
import com.sprachreise.api.repository.QcmRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.security.JwtUtil;
import com.sprachreise.api.service.PdfGeneratorService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class QcmController {

    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L, "A1", 2L, "A2", 3L, "B1", 4L, "B2", 5L, "C1", 6L, "C2"
    );

    private static final int MIN_QUESTIONS = 5;
    private static final int MAX_QUESTIONS = 30;

    private final QcmRepository qcmRepository;
    private final QcmAttemptRepository qcmAttemptRepository;
    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final JwtUtil jwtUtil;

    @Value("${storage.upload-dir}")
    private String storageDir;

    public QcmController(QcmRepository qcmRepository,
                         QcmAttemptRepository qcmAttemptRepository,
                         UserRepository userRepository,
                         TrainerProfileRepository trainerProfileRepository,
                         PdfGeneratorService pdfGeneratorService,
                         JwtUtil jwtUtil) {
        this.qcmRepository = qcmRepository;
        this.qcmAttemptRepository = qcmAttemptRepository;
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.jwtUtil = jwtUtil;
    }

    // ===== LEGACY READ ENDPOINTS (kept for backward compat) =====

    @GetMapping("/api/qcm")
    public ResponseEntity<List<QcmDto>> getAllLegacy(@RequestParam(required = false) Long levelId) {
        return ResponseEntity.ok(loadList(levelId));
    }

    @GetMapping("/api/qcm/{id}")
    public ResponseEntity<?> getByIdLegacy(@PathVariable Long id) {
        return qcmRepository.findById(id)
            .map(q -> ResponseEntity.ok(QcmDto.from(q)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/qcm/{id}/attempt")
    public ResponseEntity<?> submitAttemptLegacy(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        return submitAttemptInternal(id, body, authHeader);
    }

    // ===== /api/qcms (NEW) =====

    @GetMapping("/api/qcms")
    public ResponseEntity<List<QcmDto>> getAll(@RequestParam(required = false) Long levelId) {
        return ResponseEntity.ok(loadList(levelId));
    }

    @GetMapping("/api/qcms/mine")
    public ResponseEntity<?> getMine() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        List<QcmDto> dtos = qcmRepository.findByCreatedByOrderByIdDesc(me.getId()).stream()
            .map(QcmDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/api/qcms/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return qcmRepository.findById(id)
            .map(q -> ResponseEntity.ok(QcmDto.from(q)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/qcms")
    @Transactional
    public ResponseEntity<?> createQcm(@RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<TrainerProfile> tpOpt = trainerProfileRepository.findByUserId(me.getId());
        if (tpOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Profil formateur introuvable"));
        }
        Long lockedLevelId = tpOpt.get().getAssignedLevelId();

        String title = trim((String) body.get("title"));
        String theme = trim((String) body.get("theme"));
        if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Titre requis"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawQuestions = (List<Map<String, Object>>) body.get("questions");
        String validationError = validateQuestions(rawQuestions);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }

        Qcm qcm = new Qcm();
        qcm.setLevelId(lockedLevelId);
        qcm.setTitle(title);
        qcm.setTheme(theme.isEmpty() ? null : theme);
        qcm.setCreatedBy(me.getId());
        qcm.setScheduledAt(parseDateTime(body.get("scheduledAt")));
        qcm.setStatus(parseQcmStatus(body.get("status")));
        qcm.setQuestions(buildQuestions(rawQuestions, qcm));

        qcmRepository.save(qcm);
        return ResponseEntity.ok(QcmDto.from(qcm));
    }

    @PutMapping("/api/qcms/{id}")
    @Transactional
    public ResponseEntity<?> updateQcm(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Qcm> opt = qcmRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Qcm qcm = opt.get();
        if (qcm.getCreatedBy() == null || !qcm.getCreatedBy().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas l'auteur de ce QCM"));
        }

        if (body.containsKey("title")) {
            String t = trim((String) body.get("title"));
            if (t.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Titre requis"));
            qcm.setTitle(t);
        }
        if (body.containsKey("theme")) {
            String t = trim((String) body.get("theme"));
            qcm.setTheme(t.isEmpty() ? null : t);
        }
        if (body.containsKey("scheduledAt")) qcm.setScheduledAt(parseDateTime(body.get("scheduledAt")));
        if (body.containsKey("status")) qcm.setStatus(parseQcmStatus(body.get("status")));

        if (body.containsKey("questions")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawQuestions = (List<Map<String, Object>>) body.get("questions");
            String validationError = validateQuestions(rawQuestions);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(Map.of("error", validationError));
            }
            // orphanRemoval will delete old questions
            if (qcm.getQuestions() != null) qcm.getQuestions().clear();
            else qcm.setQuestions(new ArrayList<>());
            List<QcmQuestion> rebuilt = buildQuestions(rawQuestions, qcm);
            qcm.getQuestions().addAll(rebuilt);
        }

        qcmRepository.save(qcm);
        return ResponseEntity.ok(QcmDto.from(qcm));
    }

    @DeleteMapping("/api/qcms/{id}")
    @Transactional
    public ResponseEntity<?> deleteQcm(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Qcm> opt = qcmRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Qcm qcm = opt.get();
        if (qcm.getCreatedBy() == null || !qcm.getCreatedBy().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas l'auteur de ce QCM"));
        }
        qcmRepository.delete(qcm);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/api/qcms/{id}/results")
    public ResponseEntity<?> getResults(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Qcm> opt = qcmRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Qcm qcm = opt.get();
        if (qcm.getCreatedBy() == null || !qcm.getCreatedBy().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas l'auteur de ce QCM"));
        }
        List<QcmAttempt> attempts = qcmAttemptRepository.findByQcmIdOrderByAttemptedAtDesc(id);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        List<Map<String, Object>> rows = attempts.stream().map(at -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", at.getId());
            row.put("learnerId", at.getLearnerId());
            String name = userRepository.findById(at.getLearnerId())
                .map(u -> ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                        + (u.getLastName() == null ? "" : u.getLastName())).trim())
                .orElse("Apprenant #" + at.getLearnerId());
            row.put("learnerName", name.isEmpty() ? ("Apprenant #" + at.getLearnerId()) : name);
            row.put("score", at.getScore());
            row.put("totalQuestions", at.getTotalQuestions());
            row.put("correctAnswers", at.getCorrectAnswers());
            row.put("durationSeconds", at.getDurationSeconds());
            row.put("attemptedAt", at.getAttemptedAt() != null ? at.getAttemptedAt().format(fmt) : null);
            return row;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "qcmId", qcm.getId(),
            "qcmTitle", qcm.getTitle(),
            "levelCode", LEVEL_CODES.getOrDefault(qcm.getLevelId(), "?"),
            "total", attempts.size(),
            "attempts", rows,
            "pdfUrl", "/api/qcms/" + qcm.getId() + "/results.pdf"
        ));
    }

    @GetMapping("/api/qcms/{id}/results.pdf")
    public ResponseEntity<Resource> getResultsPdf(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Optional<Qcm> opt = qcmRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Qcm qcm = opt.get();
        if (qcm.getCreatedBy() == null || !qcm.getCreatedBy().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<QcmAttempt> attempts = qcmAttemptRepository.findByQcmIdOrderByAttemptedAtDesc(id);
        Map<Long, String> learnerNames = new HashMap<>();
        for (QcmAttempt at : attempts) {
            userRepository.findById(at.getLearnerId()).ifPresent(u -> {
                String n = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                        + (u.getLastName() == null ? "" : u.getLastName())).trim();
                learnerNames.put(u.getId(), n.isEmpty() ? u.getEmail() : n);
            });
        }
        try {
            String relativePath = pdfGeneratorService.generateQcmResultsPdf(qcm, attempts, learnerNames);
            Path filePath = Paths.get(storageDir).resolve(relativePath).normalize();
            File file = filePath.toFile();
            if (!file.exists()) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Learner attempt endpoint (mirror of legacy under /api/qcms)
    @PostMapping("/api/qcms/{id}/attempt")
    public ResponseEntity<?> submitAttempt(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return submitAttemptInternal(id, body, authHeader);
    }

    // ===== Helpers =====

    private ResponseEntity<?> submitAttemptInternal(Long id, Map<String, Object> body, String authHeader) {
        if (authHeader == null) return ResponseEntity.status(401).build();
        String token = authHeader.replace("Bearer ", "");
        String email;
        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        int score = body.containsKey("score") ? ((Number) body.get("score")).intValue() : 0;
        int total = body.containsKey("total") ? ((Number) body.get("total")).intValue() : 0;
        int correct = body.containsKey("correct") ? ((Number) body.get("correct")).intValue() : 0;
        int duration = body.containsKey("duration") ? ((Number) body.get("duration")).intValue() : 0;

        // Persist attempt
        QcmAttempt attempt = new QcmAttempt();
        attempt.setQcmId(id);
        attempt.setLearnerId(user.getId());
        attempt.setScore(BigDecimal.valueOf(score));
        attempt.setTotalQuestions(total);
        attempt.setCorrectAnswers(correct);
        attempt.setDurationSeconds(duration);
        qcmAttemptRepository.save(attempt);

        return ResponseEntity.ok(Map.of(
            "message", "Résultat enregistré",
            "score", score,
            "total", total,
            "correct", correct,
            "duration", duration,
            "percentage", total > 0 ? Math.round((double) correct / total * 100) : 0
        ));
    }

    private List<QcmDto> loadList(Long levelId) {
        var qcms = levelId != null
            ? qcmRepository.findByLevelIdOrderByIdAsc(levelId)
            : qcmRepository.findAll();
        return qcms.stream().map(QcmDto::from).collect(Collectors.toList());
    }

    private String validateQuestions(List<Map<String, Object>> rawQuestions) {
        if (rawQuestions == null || rawQuestions.isEmpty()) return "Questions requises";
        if (rawQuestions.size() < MIN_QUESTIONS || rawQuestions.size() > MAX_QUESTIONS) {
            return "Entre " + MIN_QUESTIONS + " et " + MAX_QUESTIONS + " questions requises";
        }
        for (int i = 0; i < rawQuestions.size(); i++) {
            Map<String, Object> q = rawQuestions.get(i);
            String text = trim((String) q.get("text"));
            if (text.isEmpty()) return "Question #" + (i + 1) + " : texte requis";
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) q.get("choices");
            if (choices == null || choices.size() < 2) {
                return "Question #" + (i + 1) + " : au moins 2 choix requis";
            }
            long correctCount = choices.stream().filter(c -> Boolean.TRUE.equals(c.get("isCorrect"))).count();
            if (correctCount < 1) return "Question #" + (i + 1) + " : au moins une bonne réponse requise";
        }
        return null;
    }

    private List<QcmQuestion> buildQuestions(List<Map<String, Object>> rawQuestions, Qcm qcm) {
        List<QcmQuestion> result = new ArrayList<>();
        for (int i = 0; i < rawQuestions.size(); i++) {
            Map<String, Object> rq = rawQuestions.get(i);
            QcmQuestion q = new QcmQuestion();
            q.setQcm(qcm);
            q.setQuestionText(trim((String) rq.get("text")));
            q.setOrderIndex(i + 1);
            String type = (String) rq.get("type");
            try {
                q.setQuestionType(type != null
                    ? QcmQuestion.QuestionType.valueOf(type.toUpperCase())
                    : QcmQuestion.QuestionType.SINGLE_CHOICE);
            } catch (IllegalArgumentException e) {
                q.setQuestionType(QcmQuestion.QuestionType.SINGLE_CHOICE);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawChoices = (List<Map<String, Object>>) rq.get("choices");
            List<QcmChoice> choices = new ArrayList<>();
            if (rawChoices != null) {
                for (Map<String, Object> rc : rawChoices) {
                    QcmChoice c = new QcmChoice();
                    c.setQuestion(q);
                    c.setChoiceText(trim((String) rc.get("text")));
                    c.setIsCorrect(Boolean.TRUE.equals(rc.get("isCorrect")));
                    String exp = (String) rc.get("explanation");
                    c.setExplanation(exp == null || exp.trim().isEmpty() ? null : exp.trim());
                    choices.add(c);
                }
            }
            q.setChoices(choices);
            result.add(q);
        }
        return result;
    }

    private Qcm.QcmStatus parseQcmStatus(Object raw) {
        if (raw == null) return Qcm.QcmStatus.DRAFT;
        try {
            return Qcm.QcmStatus.valueOf(String.valueOf(raw).toUpperCase());
        } catch (IllegalArgumentException e) {
            return Qcm.QcmStatus.DRAFT;
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
