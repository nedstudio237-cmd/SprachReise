package com.sprachreise.api.controller;

import com.sprachreise.api.dto.QcmDto;
import com.sprachreise.api.entity.QcmAttempt;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.QcmAttemptRepository;
import com.sprachreise.api.repository.QcmRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class QcmController {

    private final QcmRepository       qcmRepository;
    private final QcmAttemptRepository qcmAttemptRepository;
    private final UserRepository      userRepository;
    private final JwtUtil             jwtUtil;

    public QcmController(QcmRepository qcmRepository,
                         QcmAttemptRepository qcmAttemptRepository,
                         UserRepository userRepository,
                         JwtUtil jwtUtil) {
        this.qcmRepository       = qcmRepository;
        this.qcmAttemptRepository = qcmAttemptRepository;
        this.userRepository      = userRepository;
        this.jwtUtil             = jwtUtil;
    }

    // Legacy path kept for backward compat
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
    public ResponseEntity<?> submitAttemptLegacy(@PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {
        return submitAttemptInternal(id, body, authHeader);
    }

    @GetMapping("/api/qcms")
    public ResponseEntity<List<QcmDto>> getAll(@RequestParam(required = false) Long levelId) {
        return ResponseEntity.ok(loadList(levelId));
    }

    @GetMapping("/api/qcms/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return qcmRepository.findById(id)
            .map(q -> ResponseEntity.ok(QcmDto.from(q)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/qcms/{id}/attempt")
    public ResponseEntity<?> submitAttempt(@PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return submitAttemptInternal(id, body, authHeader);
    }

    private ResponseEntity<?> submitAttemptInternal(Long id, Map<String, Object> body, String authHeader) {
        if (authHeader == null) return ResponseEntity.status(401).build();
        String token = authHeader.replace("Bearer ", "");
        String email;
        try { email = jwtUtil.extractEmail(token); }
        catch (Exception e) { return ResponseEntity.status(401).build(); }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        int score    = body.containsKey("score")    ? ((Number) body.get("score")).intValue()    : 0;
        int total    = body.containsKey("total")    ? ((Number) body.get("total")).intValue()    : 0;
        int correct  = body.containsKey("correct")  ? ((Number) body.get("correct")).intValue()  : 0;
        int duration = body.containsKey("duration") ? ((Number) body.get("duration")).intValue() : 0;

        QcmAttempt attempt = new QcmAttempt();
        attempt.setQcmId(id);
        attempt.setLearnerId(user.getId());
        attempt.setScore(BigDecimal.valueOf(score));
        attempt.setTotalQuestions(total);
        attempt.setCorrectAnswers(correct);
        attempt.setDurationSeconds(duration);
        qcmAttemptRepository.save(attempt);

        return ResponseEntity.ok(Map.of(
            "message",    "Résultat enregistré",
            "score",      score,
            "total",      total,
            "correct",    correct,
            "duration",   duration,
            "percentage", total > 0 ? Math.round((double) correct / total * 100) : 0
        ));
    }

    private List<QcmDto> loadList(Long levelId) {
        var qcms = levelId != null
            ? qcmRepository.findByLevelIdOrderByIdAsc(levelId)
            : qcmRepository.findAll();
        return qcms.stream().map(QcmDto::from).collect(Collectors.toList());
    }
}
