package com.sprachreise.api.controller;

import com.sprachreise.api.dto.SessionDto;
import com.sprachreise.api.repository.StreamingSessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final StreamingSessionRepository sessionRepository;

    public SessionController(StreamingSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @GetMapping
    public ResponseEntity<List<SessionDto>> getAll(@RequestParam(required = false) Long levelId) {
        var sessions = levelId != null
            ? sessionRepository.findByLevelId(levelId)
            : sessionRepository.findUpcomingAndLive();

        return ResponseEntity.ok(
            sessions.stream().map(SessionDto::from).collect(Collectors.toList())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return sessionRepository.findById(id)
            .map(s -> ResponseEntity.ok(SessionDto.from(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/attend")
    public ResponseEntity<?> attend(@PathVariable Long id,
                                    @RequestHeader("Authorization") String authHeader) {
        return sessionRepository.findById(id)
            .map(s -> ResponseEntity.ok(Map.of(
                "message", "Inscription confirmée",
                "sessionId", id,
                "sessionTitle", s.getTitle()
            )))
            .orElse(ResponseEntity.notFound().build());
    }
}
