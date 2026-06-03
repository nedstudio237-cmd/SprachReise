package com.sprachreise.api.controller;

import com.sprachreise.api.dto.MessageDto;
import com.sprachreise.api.dto.StudentDetailDto;
import com.sprachreise.api.dto.StudentSummaryDto;
import com.sprachreise.api.entity.LearnerProgress;
import com.sprachreise.api.entity.Message;
import com.sprachreise.api.entity.QcmAttempt;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.SessionAttendee;
import com.sprachreise.api.entity.StreamingSession;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.LearnerProgressRepository;
import com.sprachreise.api.repository.MessageRepository;
import com.sprachreise.api.repository.QcmAttemptRepository;
import com.sprachreise.api.repository.SessionAttendeeRepository;
import com.sprachreise.api.repository.StreamingSessionRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trainer/students")
public class TrainerStudentsController {

    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L, "A1", 2L, "A2", 3L, "B1", 4L, "B2", 5L, "C1", 6L, "C2"
    );

    private final TrainerProfileRepository trainerProfileRepository;
    private final UserRepository userRepository;
    private final LearnerProgressRepository learnerProgressRepository;
    private final QcmAttemptRepository qcmAttemptRepository;
    private final SessionAttendeeRepository sessionAttendeeRepository;
    private final StreamingSessionRepository sessionRepository;
    private final MessageRepository messageRepository;

    public TrainerStudentsController(TrainerProfileRepository trainerProfileRepository,
                                     UserRepository userRepository,
                                     LearnerProgressRepository learnerProgressRepository,
                                     QcmAttemptRepository qcmAttemptRepository,
                                     SessionAttendeeRepository sessionAttendeeRepository,
                                     StreamingSessionRepository sessionRepository,
                                     MessageRepository messageRepository) {
        this.trainerProfileRepository = trainerProfileRepository;
        this.userRepository = userRepository;
        this.learnerProgressRepository = learnerProgressRepository;
        this.qcmAttemptRepository = qcmAttemptRepository;
        this.sessionAttendeeRepository = sessionAttendeeRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String status) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<TrainerProfile> tpOpt = trainerProfileRepository.findByUserId(me.getId());
        if (tpOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        Long levelId = tpOpt.get().getAssignedLevelId();
        String levelCode = LEVEL_CODES.getOrDefault(levelId, "?");

        List<LearnerProgress> progresses = learnerProgressRepository.findAllByLevelIdOrderByCompletionPercentageDesc(levelId);
        List<StudentSummaryDto> result = new ArrayList<>();
        for (LearnerProgress p : progresses) {
            User u = userRepository.findById(p.getLearnerId()).orElse(null);
            if (u == null || u.getRole() != Role.LEARNER) continue;
            StudentSummaryDto dto = new StudentSummaryDto();
            dto.id = u.getId();
            dto.firstName = u.getFirstName();
            dto.lastName = u.getLastName();
            dto.photoUrl = u.getPhotoUrl();
            dto.levelCode = levelCode;
            dto.completionPercentage = p.getCompletionPercentage() == null ? BigDecimal.ZERO : p.getCompletionPercentage();
            dto.certified = Boolean.TRUE.equals(p.getCertified());
            dto.lastActiveAt = u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : null;
            dto.status = computeStatus(dto.completionPercentage, dto.certified, u.getUpdatedAt());
            result.add(dto);
        }

        if (status != null && !status.isBlank()) {
            String s = status.trim().toUpperCase();
            result = result.stream().filter(d -> s.equals(d.status)).collect(Collectors.toList());
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<TrainerProfile> tpOpt = trainerProfileRepository.findByUserId(me.getId());
        if (tpOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "totalStudents", 0,
                "retentionRate", 0,
                "avgScore", 0,
                "avgProgress", 0
            ));
        }
        Long levelId = tpOpt.get().getAssignedLevelId();
        List<LearnerProgress> progresses = learnerProgressRepository.findAllByLevelIdOrderByCompletionPercentageDesc(levelId);

        int total = progresses.size();
        int active = 0;
        BigDecimal sumScore = BigDecimal.ZERO;
        BigDecimal sumProgress = BigDecimal.ZERO;

        for (LearnerProgress p : progresses) {
            BigDecimal completion = p.getCompletionPercentage() == null ? BigDecimal.ZERO : p.getCompletionPercentage();
            boolean certified = Boolean.TRUE.equals(p.getCertified());
            if (certified || completion.compareTo(BigDecimal.ZERO) > 0) active++;
            sumScore = sumScore.add(p.getQcmAvgScore() == null ? BigDecimal.ZERO : p.getQcmAvgScore());
            sumProgress = sumProgress.add(completion);
        }

        BigDecimal retentionRate = total > 0
            ? BigDecimal.valueOf(active * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal avgScore = total > 0
            ? sumScore.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal avgProgress = total > 0
            ? sumProgress.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return ResponseEntity.ok(Map.of(
            "totalStudents", total,
            "retentionRate", retentionRate,
            "avgScore", avgScore,
            "avgProgress", avgProgress
        ));
    }

    @GetMapping("/{learnerId}")
    public ResponseEntity<?> detail(@PathVariable Long learnerId) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        Optional<TrainerProfile> tpOpt = trainerProfileRepository.findByUserId(me.getId());
        if (tpOpt.isEmpty()) return ResponseEntity.notFound().build();

        Long levelId = tpOpt.get().getAssignedLevelId();
        String levelCode = LEVEL_CODES.getOrDefault(levelId, "?");

        User learner = userRepository.findById(learnerId).orElse(null);
        if (learner == null || learner.getRole() != Role.LEARNER) {
            return ResponseEntity.notFound().build();
        }

        LearnerProgress progress = learnerProgressRepository
            .findByLearnerIdAndLevelId(learnerId, levelId)
            .orElse(null);

        StudentDetailDto dto = new StudentDetailDto();
        dto.id = learner.getId();
        dto.firstName = learner.getFirstName();
        dto.lastName = learner.getLastName();
        dto.email = learner.getEmail();
        dto.photoUrl = learner.getPhotoUrl();
        dto.levelCode = levelCode;
        dto.completionPercentage = progress != null && progress.getCompletionPercentage() != null
            ? progress.getCompletionPercentage() : BigDecimal.ZERO;
        dto.certified = progress != null && Boolean.TRUE.equals(progress.getCertified());
        dto.lastActiveAt = learner.getUpdatedAt() != null ? learner.getUpdatedAt().toString() : null;
        dto.status = computeStatus(dto.completionPercentage, dto.certified, learner.getUpdatedAt());

        dto.coursesCompleted = progress != null && progress.getCoursesCompleted() != null
            ? progress.getCoursesCompleted() : 0;
        dto.totalMinutes = progress != null && progress.getTotalMinutes() != null
            ? progress.getTotalMinutes() : 0;
        dto.qcmAvgScore = progress != null && progress.getQcmAvgScore() != null
            ? progress.getQcmAvgScore() : BigDecimal.ZERO;

        // QCM attempts count for this learner
        List<QcmAttempt> allAttempts = qcmAttemptRepository.findAll();
        int qcmCount = 0;
        for (QcmAttempt a : allAttempts) {
            if (a.getLearnerId() != null && a.getLearnerId().equals(learnerId)) qcmCount++;
        }
        dto.qcmAttemptsCount = qcmCount;

        // Sessions attended (sessions belonging to current trainer that this learner attended)
        List<StreamingSession> mySessions = sessionRepository.findByTrainerIdOrderByScheduledStartDesc(me.getId());
        Set<Long> mySessionIds = new HashSet<>();
        for (StreamingSession s : mySessions) mySessionIds.add(s.getId());

        int attended = 0;
        for (Long sid : mySessionIds) {
            Optional<SessionAttendee> att = sessionAttendeeRepository.findBySessionIdAndLearnerId(sid, learnerId);
            if (att.isPresent()) attended++;
        }
        dto.sessionsAttended = attended;

        // Recent messages (last 10) between current trainer and this learner
        List<Message> messages = messageRepository.findByPair(me.getId(), learnerId);
        List<Message> last10 = messages.size() > 10
            ? messages.subList(messages.size() - 10, messages.size())
            : messages;
        dto.recentMessages = last10.stream().map(MessageDto::from).collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }

    private String computeStatus(BigDecimal completion, Boolean certified, LocalDateTime updatedAt) {
        if (Boolean.TRUE.equals(certified)) return "CERTIFIED";
        BigDecimal c = completion == null ? BigDecimal.ZERO : completion;
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        if (c.compareTo(BigDecimal.valueOf(25)) < 0 && updatedAt != null && updatedAt.isBefore(thirtyDaysAgo)) {
            return "LATE";
        }
        if (c.compareTo(BigDecimal.ZERO) > 0) return "ACTIVE";
        return "ACTIVE";
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) return null;
        String email = ((UserDetails) principal).getUsername();
        return userRepository.findByEmail(email).orElse(null);
    }
}
