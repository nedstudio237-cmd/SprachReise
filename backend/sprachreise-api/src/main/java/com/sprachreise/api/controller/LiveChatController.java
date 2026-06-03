package com.sprachreise.api.controller;

import com.sprachreise.api.entity.StreamingSession;
import com.sprachreise.api.repository.StreamingSessionRepository;
import com.sprachreise.api.repository.UserRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class LiveChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository             userRepository;
    private final StreamingSessionRepository sessionRepository;

    public LiveChatController(SimpMessagingTemplate messagingTemplate,
                              UserRepository userRepository,
                              StreamingSessionRepository sessionRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository    = userRepository;
        this.sessionRepository = sessionRepository;
    }

    // ── Message de chat ───────────────────────────────────────────────────────
    // Client envoie à /app/live/{sessionId}/chat
    // Reçu par /topic/live/{sessionId}/chat
    @MessageMapping("/live/{sessionId}/chat")
    @SendTo("/topic/live/{sessionId}/chat")
    public Map<String, Object> handleChat(@DestinationVariable Long sessionId,
                                          Map<String, Object> payload,
                                          Principal principal) {
        String senderName = resolveName(principal);
        return Map.of(
            "type",      "CHAT",
            "sessionId", sessionId,
            "sender",    senderName,
            "message",   payload.getOrDefault("message", "").toString(),
            "timestamp", LocalDateTime.now().toString()
        );
    }

    // ── Note personnelle (visible seulement par l'étudiant) ───────────────────
    // Client envoie à /app/live/{sessionId}/note
    // Reçu par /topic/live/{sessionId}/notes (broadcast pour stocker côté client)
    @MessageMapping("/live/{sessionId}/note")
    @SendTo("/topic/live/{sessionId}/notes")
    public Map<String, Object> handleNote(@DestinationVariable Long sessionId,
                                          Map<String, Object> payload,
                                          Principal principal) {
        String senderName = resolveName(principal);
        return Map.of(
            "type",      "NOTE",
            "sessionId", sessionId,
            "author",    senderName,
            "content",   payload.getOrDefault("content", "").toString(),
            "timestamp", LocalDateTime.now().toString()
        );
    }

    // ── Signal de présence (join/leave) ───────────────────────────────────────
    @MessageMapping("/live/{sessionId}/presence")
    @SendTo("/topic/live/{sessionId}/presence")
    public Map<String, Object> handlePresence(@DestinationVariable Long sessionId,
                                              Map<String, Object> payload,
                                              Principal principal) {
        String name  = resolveName(principal);
        String event = payload.getOrDefault("event", "JOIN").toString(); // JOIN | LEAVE
        return Map.of(
            "type",      "PRESENCE",
            "sessionId", sessionId,
            "user",      name,
            "event",     event,
            "timestamp", LocalDateTime.now().toString()
        );
    }

    // ── Changement d'état de la session (trainer uniquement) ─────────────────
    // Client envoie à /app/live/{sessionId}/status
    // Broadcast à /topic/live/{sessionId}/status
    @MessageMapping("/live/{sessionId}/status")
    @SendTo("/topic/live/{sessionId}/status")
    public Map<String, Object> handleStatusChange(@DestinationVariable Long sessionId,
                                                  Map<String, Object> payload,
                                                  Principal principal) {
        String newStatus = payload.getOrDefault("status", "").toString();
        sessionRepository.findById(sessionId).ifPresent(s -> {
            try {
                s.setStatus(StreamingSession.SessionStatus.valueOf(newStatus));
                sessionRepository.save(s);
            } catch (IllegalArgumentException ignored) {}
        });
        return Map.of(
            "type",      "STATUS_CHANGE",
            "sessionId", sessionId,
            "status",    newStatus,
            "timestamp", LocalDateTime.now().toString()
        );
    }

    private String resolveName(Principal principal) {
        if (principal == null) return "Anonyme";
        return userRepository.findByEmail(principal.getName())
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse(principal.getName());
    }
}
