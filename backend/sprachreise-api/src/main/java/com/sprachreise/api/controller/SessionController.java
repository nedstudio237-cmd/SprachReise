package com.sprachreise.api.controller;

import com.sprachreise.api.dto.SessionDto;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.SessionAttendee;
import com.sprachreise.api.entity.StreamingSession;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.SessionAttendeeRepository;
import com.sprachreise.api.repository.StreamingSessionRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import com.sprachreise.api.service.AgoraTokenService;
import com.sprachreise.api.service.LoggingMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L, "A1", 2L, "A2", 3L, "B1", 4L, "B2", 5L, "C1", 6L, "C2"
    );

    private static final Set<String> ALLOWED_PDF_EXT = Set.of("pdf");
    private static final long MAX_PDF_BYTES = 20L * 1024 * 1024;

    // TODO 7.6: scheduled reminder cron — to be added later (e.g. @Scheduled hourly job
    // that scans streaming_sessions with status=SCHEDULED and scheduledStart within
    // 1h / 15min windows, then calls LoggingMailService.notifyLearnersSessionScheduled
    // (or a dedicated reminder method) and pushes notifications.

    private final StreamingSessionRepository sessionRepository;
    private final SessionAttendeeRepository attendeeRepository;
    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;
    private final AgoraTokenService agoraTokenService;
    private final LoggingMailService mailService;

    @Value("${storage.upload-dir}")
    private String storageDir;

    public SessionController(StreamingSessionRepository sessionRepository,
                             SessionAttendeeRepository attendeeRepository,
                             UserRepository userRepository,
                             TrainerProfileRepository trainerProfileRepository,
                             AgoraTokenService agoraTokenService,
                             LoggingMailService mailService) {
        this.sessionRepository = sessionRepository;
        this.attendeeRepository = attendeeRepository;
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.agoraTokenService = agoraTokenService;
        this.mailService = mailService;
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

    @GetMapping("/mine")
    public ResponseEntity<?> getMine() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        List<SessionDto> dtos = sessionRepository.findByTrainerIdOrderByScheduledStartDesc(me.getId()).stream()
            .map(SessionDto::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return sessionRepository.findById(id)
            .map(s -> ResponseEntity.ok(SessionDto.from(s)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }

        Optional<TrainerProfile> tpOpt = trainerProfileRepository.findByUserId(me.getId());
        if (tpOpt.isEmpty() || tpOpt.get().getAssignedLevelId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Profil formateur sans niveau assigné"));
        }
        Long levelId = tpOpt.get().getAssignedLevelId();
        String levelCode = LEVEL_CODES.getOrDefault(levelId, "X");

        String title = strOf(body.get("title"));
        if (title.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Titre requis"));
        }
        String description = strOf(body.get("description"));

        LocalDateTime scheduledStart;
        try {
            scheduledStart = LocalDateTime.parse(strOf(body.get("scheduledStart")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduledStart invalide (ISO attendu)"));
        }

        Integer durationMinutes;
        try {
            Object dm = body.get("durationMinutes");
            durationMinutes = dm == null ? 60 : Integer.parseInt(dm.toString());
            if (durationMinutes <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "durationMinutes invalide"));
        }

        String attachmentPdf = strOf(body.get("attachmentPdf"));
        boolean recordEnabled = body.get("recordEnabled") != null
            && Boolean.parseBoolean(body.get("recordEnabled").toString());

        StreamingSession s = new StreamingSession();
        s.setTrainer(me);
        s.setLevelId(levelId);
        s.setTitle(title);
        s.setDescription(description.isEmpty() ? null : description);
        s.setScheduledStart(scheduledStart);
        s.setDurationMinutes(durationMinutes);
        s.setAgoraChannel("sr_" + levelCode + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        s.setStatus(StreamingSession.SessionStatus.SCHEDULED);
        s.setRecordEnabled(recordEnabled);
        if (!attachmentPdf.isEmpty()) s.setAttachmentPdf(attachmentPdf);

        StreamingSession saved = sessionRepository.save(s);
        mailService.notifyLearnersSessionScheduled(levelId, saved.getTitle(), saved.getScheduledStart());

        return ResponseEntity.status(HttpStatus.CREATED).body(SessionDto.from(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }

        Optional<StreamingSession> opt = sessionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        StreamingSession s = opt.get();
        if (s.getTrainer() == null || !s.getTrainer().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas propriétaire de cette session"));
        }

        boolean scheduleChanged = false;

        if (body.containsKey("title")) {
            String v = strOf(body.get("title"));
            if (v.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Titre requis"));
            s.setTitle(v);
        }
        if (body.containsKey("description")) {
            String v = strOf(body.get("description"));
            s.setDescription(v.isEmpty() ? null : v);
        }
        if (body.containsKey("scheduledStart")) {
            try {
                LocalDateTime ns = LocalDateTime.parse(strOf(body.get("scheduledStart")));
                if (!ns.equals(s.getScheduledStart())) scheduleChanged = true;
                s.setScheduledStart(ns);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "scheduledStart invalide (ISO attendu)"));
            }
        }
        if (body.containsKey("durationMinutes")) {
            try {
                int dm = Integer.parseInt(body.get("durationMinutes").toString());
                if (dm <= 0) throw new NumberFormatException();
                s.setDurationMinutes(dm);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "durationMinutes invalide"));
            }
        }
        if (body.containsKey("attachmentPdf")) {
            String v = strOf(body.get("attachmentPdf"));
            s.setAttachmentPdf(v.isEmpty() ? null : v);
        }
        if (body.containsKey("recordEnabled")) {
            s.setRecordEnabled(Boolean.parseBoolean(body.get("recordEnabled").toString()));
        }

        StreamingSession saved = sessionRepository.save(s);
        if (scheduleChanged) {
            mailService.notifyLearnersSessionScheduled(saved.getLevelId(), saved.getTitle(), saved.getScheduledStart());
        }
        return ResponseEntity.ok(SessionDto.from(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<StreamingSession> opt = sessionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        StreamingSession s = opt.get();
        if (s.getTrainer() == null || !s.getTrainer().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas propriétaire de cette session"));
        }
        s.setStatus(StreamingSession.SessionStatus.CANCELLED);
        sessionRepository.save(s);
        return ResponseEntity.ok(Map.of("status", "CANCELLED"));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> start(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<StreamingSession> opt = sessionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        StreamingSession s = opt.get();
        if (s.getTrainer() == null || !s.getTrainer().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas propriétaire de cette session"));
        }
        s.setStatus(StreamingSession.SessionStatus.LIVE);
        sessionRepository.save(s);

        String token = agoraTokenService.generateToken(s.getAgoraChannel(), me.getId(), "HOST");
        return ResponseEntity.ok(Map.of(
            "session", SessionDto.from(s),
            "agoraChannel", s.getAgoraChannel(),
            "token", token,
            "role", "HOST"
        ));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<?> end(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<StreamingSession> opt = sessionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        StreamingSession s = opt.get();
        if (s.getTrainer() == null || !s.getTrainer().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas propriétaire de cette session"));
        }
        s.setStatus(StreamingSession.SessionStatus.ENDED);
        if (Boolean.TRUE.equals(s.getRecordEnabled())) {
            s.setRecordingPath("recordings/session_" + s.getId() + ".mp4");
        }
        sessionRepository.save(s);
        return ResponseEntity.ok(SessionDto.from(s));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> join(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<StreamingSession> opt = sessionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        StreamingSession s = opt.get();

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

        String token = agoraTokenService.generateToken(s.getAgoraChannel(), me.getId(), "AUDIENCE");
        return ResponseEntity.ok(Map.of(
            "session", SessionDto.from(s),
            "agoraChannel", s.getAgoraChannel(),
            "token", token,
            "role", "AUDIENCE"
        ));
    }

    @PostMapping("/{id}/attachment")
    public ResponseEntity<?> uploadAttachment(@PathVariable Long id,
                                              @RequestParam("file") MultipartFile file) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<StreamingSession> opt = sessionRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        StreamingSession s = opt.get();
        if (s.getTrainer() == null || !s.getTrainer().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas propriétaire de cette session"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
        }
        if (file.getSize() > MAX_PDF_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "PDF > 20 Mo"));
        }
        String ext = extOf(file.getOriginalFilename());
        if (!ALLOWED_PDF_EXT.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error", "PDF : format .pdf requis"));
        }

        try {
            Path dir = Paths.get(storageDir, "sessions", "pdf");
            Files.createDirectories(dir);
            String filename = "session_" + s.getId() + "_" + System.currentTimeMillis() + ".pdf";
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String relative = "sessions/pdf/" + filename;
            s.setAttachmentPdf(relative);
            sessionRepository.save(s);
            return ResponseEntity.ok(Map.of("attachmentPdf", relative));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Échec de l'upload : " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/attend")
    public ResponseEntity<?> attend(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return sessionRepository.findById(id)
            .map(s -> ResponseEntity.ok(Map.of(
                "message", "Inscription confirmée",
                "sessionId", id,
                "sessionTitle", s.getTitle()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) return null;
        String email = ((UserDetails) principal).getUsername();
        return userRepository.findByEmail(email).orElse(null);
    }

    private static String strOf(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String extOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        return filename.substring(dot + 1).toLowerCase();
    }
}
