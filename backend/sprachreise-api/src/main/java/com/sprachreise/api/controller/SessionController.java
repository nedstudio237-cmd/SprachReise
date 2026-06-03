package com.sprachreise.api.controller;

import com.sprachreise.api.dto.SessionDto;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.SessionAttendee;
import com.sprachreise.api.entity.StreamingSession;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.SessionAttendeeRepository;
import com.sprachreise.api.repository.StreamingSessionRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.AgoraTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final StreamingSessionRepository sessionRepository;
    private final SessionAttendeeRepository  attendeeRepository;
    private final UserRepository             userRepository;
    private final AgoraTokenService          agoraTokenService;
    private final SimpMessagingTemplate      broker;

    private static final Map<String, Long> LEVEL_IDS = Map.of(
        "A1",1L,"A2",2L,"B1",3L,"B2",4L,"C1",5L,"C2",6L
    );

    public SessionController(StreamingSessionRepository sessionRepository,
                             SessionAttendeeRepository attendeeRepository,
                             UserRepository userRepository,
                             AgoraTokenService agoraTokenService,
                             SimpMessagingTemplate broker) {
        this.sessionRepository = sessionRepository;
        this.attendeeRepository = attendeeRepository;
        this.userRepository    = userRepository;
        this.agoraTokenService = agoraTokenService;
        this.broker            = broker;
    }

    // ── Liste sessions visibles par l'apprenant (formateur assigné seulement) ─
    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) Long levelId,
                                    Authentication auth) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();

        List<StreamingSession> sessions;

        if (me.getRole() == Role.LEARNER) {
            // L'apprenant voit uniquement les sessions de son formateur assigné
            if (me.getAssignedTrainerId() != null) {
                sessions = sessionRepository.findAllByTrainerId(me.getAssignedTrainerId());
            } else {
                // Pas encore assigné : afficher les sessions de son niveau
                Long lvl = levelId != null ? levelId :
                    LEVEL_IDS.getOrDefault(me.getLevelCode() != null ? me.getLevelCode() : "A1", 1L);
                sessions = sessionRepository.findByLevelId(lvl);
            }
        } else if (me.getRole() == Role.TRAINER) {
            sessions = sessionRepository.findAllByTrainerId(me.getId());
        } else {
            // ADMIN voit tout
            sessions = levelId != null
                ? sessionRepository.findByLevelId(levelId)
                : sessionRepository.findAll();
        }
        return ResponseEntity.ok(sessions.stream().map(SessionDto::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return sessionRepository.findById(id)
            .map(s -> ResponseEntity.ok(SessionDto.from(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    // ── Rejoindre une session live (obtenir token Agora) ──────────────────────
    @PostMapping("/{id}/join")
    public ResponseEntity<?> join(@PathVariable Long id, Authentication auth) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();

        StreamingSession s = sessionRepository.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();

        // Vérifier que l'apprenant appartient bien à ce formateur
        if (me.getRole() == Role.LEARNER) {
            if (me.getAssignedTrainerId() == null ||
                !me.getAssignedTrainerId().equals(s.getTrainer().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Vous n'êtes pas dans la classe de ce formateur"));
            }
        }

        SessionAttendee att = attendeeRepository.findBySessionIdAndLearnerId(id, me.getId())
            .orElseGet(() -> {
                SessionAttendee a = new SessionAttendee();
                a.setSessionId(id);
                a.setLearnerId(me.getId());
                a.setRegisteredAt(LocalDateTime.now());
                return a;
            });
        att.setJoinedAt(LocalDateTime.now());
        attendeeRepository.save(att);

        String role  = me.getRole() == Role.TRAINER ? "HOST" : "AUDIENCE";
        String token = agoraTokenService.generateToken(s.getAgoraChannel(), me.getId(), role);
        return ResponseEntity.ok(Map.of(
            "session",       SessionDto.from(s),
            "agoraChannel",  s.getAgoraChannel(),
            "token",         token,
            "role",          role,
            "userId",        me.getId(),
            "userName",      me.getFirstName() + " " + me.getLastName()
        ));
    }

    // ── Quitter une session ───────────────────────────────────────────────────
    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leave(@PathVariable Long id, Authentication auth) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();
        attendeeRepository.findBySessionIdAndLearnerId(id, me.getId()).ifPresent(att -> {
            att.setLeftAt(LocalDateTime.now());
            attendeeRepository.save(att);
        });
        return ResponseEntity.ok(Map.of("message", "Session quittée"));
    }

    // ── Changer le statut d'une session (TRAINER seulement) ──────────────────
    @PostMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          Authentication auth) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();

        StreamingSession s = sessionRepository.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        if (!s.getTrainer().getId().equals(me.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès refusé"));

        String newStatus = body.getOrDefault("status","");
        try {
            s.setStatus(StreamingSession.SessionStatus.valueOf(newStatus));
            sessionRepository.save(s);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error","Statut invalide"));
        }

        // Broadcast instantané à tous les participants via STOMP
        broker.convertAndSend(
            "/topic/live/" + id + "/status",
            Map.of("status", s.getStatus().name(), "sessionId", id)
        );

        return ResponseEntity.ok(Map.of("status", s.getStatus().name(), "sessionId", id));
    }

    // ── Participants actuels d'une session ────────────────────────────────────
    @GetMapping("/{id}/attendees")
    public ResponseEntity<?> getAttendees(@PathVariable Long id, Authentication auth) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();

        List<SessionAttendee> attendees = attendeeRepository.findBySessionId(id);
        List<Map<String,Object>> result = new ArrayList<>();
        for (SessionAttendee a : attendees) {
            userRepository.findById(a.getLearnerId()).ifPresent(u -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("userId",    u.getId());
                m.put("firstName", u.getFirstName());
                m.put("lastName",  u.getLastName());
                m.put("joinedAt",  a.getJoinedAt() != null ? a.getJoinedAt().toString() : null);
                m.put("leftAt",    a.getLeftAt()   != null ? a.getLeftAt().toString()   : null);
                m.put("online",    a.getLeftAt() == null && a.getJoinedAt() != null);
                result.add(m);
            });
        }
        return ResponseEntity.ok(result);
    }

    // ── Stats d'une session terminée (formateur + apprenants assignés) ────────
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getStats(@PathVariable Long id, Authentication auth) {
        User me = currentUser(auth);
        if (me == null) return ResponseEntity.status(401).build();

        StreamingSession s = sessionRepository.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();
        boolean isTrainer = me.getRole() == Role.TRAINER && s.getTrainer().getId().equals(me.getId());
        boolean isAssignedLearner = me.getRole() == Role.LEARNER
            && s.getTrainer().getId().equals(me.getAssignedTrainerId());
        if (!isTrainer && !isAssignedLearner && me.getRole() != Role.ADMIN)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès refusé"));

        List<SessionAttendee> attendees = attendeeRepository.findBySessionId(id);

        long totalJoined = attendees.stream().filter(a -> a.getJoinedAt() != null).count();
        long stayedFull  = attendees.stream().filter(a ->
            a.getJoinedAt() != null && a.getLeftAt() == null).count();

        // Durée moyenne en minutes
        double avgDuration = attendees.stream()
            .filter(a -> a.getJoinedAt() != null && a.getLeftAt() != null)
            .mapToLong(a -> java.time.Duration.between(a.getJoinedAt(), a.getLeftAt()).toMinutes())
            .average().orElse(0);

        long registered = attendees.size();

        Map<String,Object> stats = new LinkedHashMap<>();
        stats.put("sessionTitle",        s.getTitle());
        stats.put("scheduledDuration",   s.getDurationMinutes());
        stats.put("totalRegistered",     registered);
        stats.put("totalJoined",         totalJoined);
        stats.put("stayedUntilEnd",      stayedFull);
        stats.put("avgDurationMinutes",  Math.round(avgDuration));
        stats.put("attendanceRate",      registered > 0 ? Math.round(100.0 * totalJoined / registered) : 0);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/attend")
    public ResponseEntity<?> attend(@PathVariable Long id) {
        return sessionRepository.findById(id)
            .map(s -> ResponseEntity.ok(Map.of(
                "message", "Inscription confirmée",
                "sessionId", id,
                "sessionTitle", s.getTitle())))
            .orElse(ResponseEntity.notFound().build());
    }

    private User currentUser(Authentication auth) {
        if (auth == null) return null;
        Object p = auth.getPrincipal();
        if (!(p instanceof UserDetails ud)) return null;
        return userRepository.findByEmail(ud.getUsername()).orElse(null);
    }
}
