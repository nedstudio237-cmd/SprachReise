package com.sprachreise.api.controller;

import com.sprachreise.api.dto.QcmDto;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.QcmRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/qcm")
public class QcmController {

    private final QcmRepository qcmRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public QcmController(QcmRepository qcmRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.qcmRepository = qcmRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping
    public ResponseEntity<List<QcmDto>> getAll(@RequestParam(required = false) Long levelId) {
        var qcms = levelId != null
            ? qcmRepository.findByLevelIdOrderByIdAsc(levelId)
            : qcmRepository.findAll();

        return ResponseEntity.ok(
            qcms.stream().map(QcmDto::from).collect(Collectors.toList())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return qcmRepository.findById(id)
            .map(q -> ResponseEntity.ok(QcmDto.from(q)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/attempt")
    public ResponseEntity<?> submitAttempt(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        int score = body.containsKey("score") ? ((Number) body.get("score")).intValue() : 0;
        int total = body.containsKey("total") ? ((Number) body.get("total")).intValue() : 0;
        int correct = body.containsKey("correct") ? ((Number) body.get("correct")).intValue() : 0;
        int duration = body.containsKey("duration") ? ((Number) body.get("duration")).intValue() : 0;

        return ResponseEntity.ok(Map.of(
            "message", "Résultat enregistré",
            "score", score,
            "total", total,
            "correct", correct,
            "duration", duration,
            "percentage", total > 0 ? Math.round((double) correct / total * 100) : 0
        ));
    }
}
