package com.sprachreise.api.controller;

import com.sprachreise.api.entity.*;
import com.sprachreise.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/trainer")
public class TrainerController {

    private static final Map<String, Long> LEVEL_IDS = Map.of(
        "A1",1L,"A2",2L,"B1",3L,"B2",4L,"C1",5L,"C2",6L
    );
    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L,"A1",2L,"A2",3L,"B1",4L,"B2",5L,"C1",6L,"C2"
    );

    private final UserRepository             userRepository;
    private final TrainerProfileRepository   trainerProfileRepository;
    private final CourseRepository           courseRepository;
    private final StreamingSessionRepository sessionRepository;
    private final ExamRepository             examRepository;
    private final ExamSubmissionRepository   submissionRepository;
    private final QcmRepository              qcmRepository;
    private final LearnerProgressRepository  progressRepository;
    private final PasswordEncoder            passwordEncoder;
    private final NotificationRepository     notificationRepository;

    public TrainerController(UserRepository userRepository,
                             TrainerProfileRepository trainerProfileRepository,
                             CourseRepository courseRepository,
                             StreamingSessionRepository sessionRepository,
                             ExamRepository examRepository,
                             ExamSubmissionRepository submissionRepository,
                             QcmRepository qcmRepository,
                             LearnerProgressRepository progressRepository,
                             PasswordEncoder passwordEncoder,
                             NotificationRepository notificationRepository) {
        this.userRepository           = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
        this.courseRepository         = courseRepository;
        this.sessionRepository        = sessionRepository;
        this.examRepository           = examRepository;
        this.submissionRepository     = submissionRepository;
        this.qcmRepository            = qcmRepository;
        this.progressRepository       = progressRepository;
        this.passwordEncoder          = passwordEncoder;
        this.notificationRepository   = notificationRepository;
    }

    // ── Notification helper ───────────────────────────────────────────────────
    private void notifyLearners(Long trainerId, Long levelId, String type, String title, String body, Long entityId) {
        userRepository.findAll().stream()
            .filter(u -> u.getRole() == Role.LEARNER && trainerId.equals(u.getAssignedTrainerId()))
            .forEach(learner -> {
                Notification n = new Notification();
                n.setUserId(learner.getId());
                n.setType(type);
                n.setTitle(title);
                n.setBody(body);
                n.setEntityId(entityId);
                notificationRepository.save(n);
            });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private User currentUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private ResponseEntity<?> forbiddenIfNotTrainer(User user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","Non authentifié"));
        if (user.getRole() != Role.TRAINER) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès formateurs uniquement"));
        return null;
    }

    private TrainerProfile getApprovedProfile(Long userId) {
        return trainerProfileRepository.findByUserId(userId)
                .filter(p -> p.getStatus() == TrainerProfile.Status.APPROVED)
                .orElse(null);
    }

    private String trim(Map<String, Object> body, String key) {
        Object v = body == null ? null : body.get(key);
        return v == null ? "" : v.toString().trim();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7.3 PROFIL
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        TrainerProfile p = trainerProfileRepository.findByUserId(user.getId()).orElse(null);
        if (p == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","Profil introuvable"));

        List<Course> courses = courseRepository.findAllByTrainerId(user.getId());
        long published = courses.stream().filter(c -> c.getStatus() == Course.CourseStatus.PUBLISHED).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId",          user.getId());
        result.put("firstName",        user.getFirstName());
        result.put("lastName",         user.getLastName());
        result.put("email",            user.getEmail());
        result.put("phone",            user.getPhone());
        result.put("bio",              user.getBio());
        result.put("photoUrl",         user.getPhotoUrl());
        result.put("profileId",        p.getId());
        result.put("teachingLevel",    p.getTeachingLevelCode());
        result.put("teachingLanguage", p.getTeachingLanguageCode());
        result.put("nativeLanguage",   p.getNativeLanguage());
        result.put("maxStudents",      p.getMaxStudents());
        result.put("ratingAvg",        p.getRatingAvg());
        result.put("status",           p.getStatus().name());
        result.put("motivation",       p.getMotivation());
        result.put("totalCourses",     courses.size());
        result.put("publishedCourses", published);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/profile")
    @Transactional
    public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody Map<String, Object> body) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        String bio   = trim(body, "bio");
        String phone = trim(body, "phone");
        if (!bio.isEmpty())   user.setBio(bio);
        if (!phone.isEmpty()) user.setPhone(phone);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message","Profil mis à jour"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7.4 COURS
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/courses")
    public ResponseEntity<?> myCourses(Authentication auth) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        return ResponseEntity.ok(courseRepository.findAllByTrainerId(user.getId()).stream().map(this::courseToMap).toList());
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(Authentication auth, @RequestBody Map<String, Object> body) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        TrainerProfile p = getApprovedProfile(user.getId());
        if (p == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error","Candidature non approuvée"));

        String title = trim(body,"title");
        if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Titre requis"));

        Long levelId = LEVEL_IDS.getOrDefault(p.getTeachingLevelCode(), 1L);
        Course course = new Course();
        course.setTrainer(user);
        course.setLevelId(levelId);
        course.setTitle(title);
        course.setDescription(trim(body,"description").isEmpty() ? null : trim(body,"description"));
        course.setTheme(trim(body,"theme").isEmpty() ? null : trim(body,"theme"));
        String vp = trim(body,"videoPath");
        course.setVideoPath(vp.isEmpty() ? null : vp);
        String pp = trim(body,"pdfPath");
        course.setPdfPath(pp.isEmpty() ? null : pp);
        course.setStatus(Course.CourseStatus.DRAFT);
        courseRepository.save(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(courseToMap(course));
    }

    @PostMapping("/courses/{id}/publish")
    public ResponseEntity<?> togglePublish(Authentication auth, @PathVariable Long id) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        Course c = courseRepository.findById(id).orElse(null);
        if (c == null || !c.getTrainer().getId().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès refusé"));
        boolean wasPublished = c.getStatus() == Course.CourseStatus.PUBLISHED;
        c.setStatus(wasPublished ? Course.CourseStatus.DRAFT : Course.CourseStatus.PUBLISHED);
        courseRepository.save(c);
        if (!wasPublished) {
            notifyLearners(user.getId(), c.getLevelId(), "COURSE",
                "Nouveau cours disponible",
                user.getFirstName() + " a publié : " + c.getTitle(), c.getId());
        }
        return ResponseEntity.ok(Map.of("status", c.getStatus().name()));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7.5 QCM & ÉPREUVES
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/exams")
    public ResponseEntity<?> myExams(Authentication auth) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        List<Exam> exams = examRepository.findByTrainerIdOrderByIdDesc(user.getId());
        List<Map<String,Object>> result = new ArrayList<>();
        for (Exam e : exams) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id",           e.getId());
            m.put("title",        e.getTitle());
            m.put("instructions", e.getInstructions());
            m.put("status",       e.getStatus().name());
            m.put("levelId",      e.getLevelId());
            m.put("scheduledAt",  e.getScheduledAt() != null ? e.getScheduledAt().toString() : null);
            m.put("submissions",  submissionRepository.findByExamIdOrderBySubmittedAtDesc(e.getId()).size());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/exams")
    @Transactional
    public ResponseEntity<?> createExam(Authentication auth, @RequestBody Map<String, Object> body) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        TrainerProfile p = getApprovedProfile(user.getId());
        if (p == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Non approuvé"));

        String title = trim(body,"title");
        if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Titre requis"));

        Long levelId = LEVEL_IDS.getOrDefault(p.getTeachingLevelCode(), 1L);
        Exam exam = new Exam();
        exam.setTrainerId(user.getId());
        exam.setLevelId(levelId);
        exam.setTitle(title);
        exam.setInstructions(trim(body,"instructions").isEmpty() ? null : trim(body,"instructions"));
        exam.setStatus(Exam.ExamStatus.DRAFT);
        exam.setCreatedAt(LocalDateTime.now());

        String scheduledStr = trim(body,"scheduledAt");
        if (!scheduledStr.isEmpty()) {
            try { exam.setScheduledAt(LocalDateTime.parse(scheduledStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)); }
            catch (Exception ignored) {}
        }
        examRepository.save(exam);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id",exam.getId(),"title",exam.getTitle(),"status",exam.getStatus().name()));
    }

    @PostMapping("/exams/{id}/publish")
    @Transactional
    public ResponseEntity<?> publishExam(Authentication auth, @PathVariable Long id) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        Exam exam = examRepository.findById(id).orElse(null);
        if (exam == null || !exam.getTrainerId().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès refusé"));
        exam.setStatus(exam.getStatus() == Exam.ExamStatus.PUBLISHED ? Exam.ExamStatus.DRAFT : Exam.ExamStatus.PUBLISHED);
        examRepository.save(exam);
        return ResponseEntity.ok(Map.of("status",exam.getStatus().name()));
    }

    @GetMapping("/exams/{id}/submissions")
    public ResponseEntity<?> examSubmissions(Authentication auth, @PathVariable Long id) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        Exam exam = examRepository.findById(id).orElse(null);
        if (exam == null || !exam.getTrainerId().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès refusé"));

        var subs = submissionRepository.findByExamIdOrderBySubmittedAtDesc(id);
        List<Map<String,Object>> result = new ArrayList<>();
        for (var s : subs) {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id",          s.getId());
            m.put("learnerId",   s.getLearnerId());
            m.put("submittedAt", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : null);
            m.put("grade",       s.getGrade());
            m.put("feedback",    s.getFeedback());
            userRepository.findById(s.getLearnerId()).ifPresent(u -> {
                m.put("learnerName", u.getFirstName() + " " + u.getLastName());
                m.put("learnerEmail", u.getEmail());
            });
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7.6 SESSIONS LIVE
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/sessions")
    public ResponseEntity<?> mySessions(Authentication auth) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        List<StreamingSession> sessions = sessionRepository.findByTrainerIdOrderByScheduledStartDesc(user.getId());
        return ResponseEntity.ok(sessions.stream().map(this::sessionToMap).toList());
    }

    @PostMapping("/sessions")
    @Transactional
    public ResponseEntity<?> createSession(Authentication auth, @RequestBody Map<String, Object> body) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        TrainerProfile p = getApprovedProfile(user.getId());
        if (p == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Non approuvé"));

        String title = trim(body,"title");
        if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Titre requis"));

        String scheduledStr = trim(body,"scheduledAt");
        LocalDateTime scheduledAt;
        try { scheduledAt = LocalDateTime.parse(scheduledStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error","Date invalide (format: yyyy-MM-ddTHH:mm:ss)")); }

        Object durObj = body.get("durationMinutes");
        int duration = (durObj instanceof Number n) ? n.intValue() : 60;

        Long levelId = LEVEL_IDS.getOrDefault(p.getTeachingLevelCode(), 1L);

        StreamingSession session = new StreamingSession();
        session.setTrainer(user);
        session.setLevelId(levelId);
        session.setTitle(title);
        String desc = trim(body,"description");
        session.setDescription(desc.isEmpty() ? null : desc);
        session.setScheduledStart(scheduledAt);
        session.setDurationMinutes(duration);
        session.setAgoraChannel("sr-" + user.getId() + "-" + System.currentTimeMillis());
        session.setStatus(StreamingSession.SessionStatus.SCHEDULED);
        sessionRepository.save(session);
        notifyLearners(user.getId(), session.getLevelId(), "SESSION",
            "Nouvelle session live programmée",
            user.getFirstName() + " a programmé : " + session.getTitle(), session.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionToMap(session));
    }

    @PostMapping("/sessions/{id}/cancel")
    @Transactional
    public ResponseEntity<?> cancelSession(Authentication auth, @PathVariable Long id) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        StreamingSession session = sessionRepository.findById(id).orElse(null);
        if (session == null || !session.getTrainer().getId().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès refusé"));
        session.setStatus(StreamingSession.SessionStatus.CANCELLED);
        sessionRepository.save(session);
        return ResponseEntity.ok(Map.of("status","CANCELLED"));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7.7 SUIVI APPRENANTS
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/learners")
    public ResponseEntity<?> myLearners(Authentication auth) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        TrainerProfile p = trainerProfileRepository.findByUserId(user.getId()).orElse(null);
        if (p == null) return ResponseEntity.ok(List.of());

        Long levelId = LEVEL_IDS.getOrDefault(p.getTeachingLevelCode(), 1L);
        List<LearnerProgress> progresses = progressRepository.findAllByLevelIdOrderByCompletionPercentageDesc(levelId);

        List<Map<String,Object>> result = new ArrayList<>();
        for (LearnerProgress progress : progresses) {
            userRepository.findById(progress.getLearnerId()).ifPresent(u -> {
                if (u.getRole() != Role.LEARNER) return;
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("learnerId",           u.getId());
                m.put("firstName",            u.getFirstName());
                m.put("lastName",             u.getLastName());
                m.put("email",                u.getEmail());
                m.put("active",               u.getActive());
                m.put("completionPercentage", progress.getCompletionPercentage());
                m.put("coursesCompleted",     progress.getCoursesCompleted());
                m.put("sessionsAttended",     progress.getSessionsAttended());
                m.put("qcmAvgScore",          progress.getQcmAvgScore());
                m.put("totalMinutes",         progress.getTotalMinutes());
                m.put("certified",            progress.getCertified());
                // Statut : certifié, actif, en retard (< 30% après 7 jours)
                String status = "ACTIF";
                if (Boolean.TRUE.equals(progress.getCertified())) status = "CERTIFIE";
                else if (progress.getCompletionPercentage() != null && progress.getCompletionPercentage().doubleValue() < 10) status = "EN_RETARD";
                m.put("learnerStatus", status);
                result.add(m);
            });
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/learners/{learnerId}")
    public ResponseEntity<?> learnerDetail(Authentication auth, @PathVariable Long learnerId) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        User learner = userRepository.findById(learnerId).orElse(null);
        if (learner == null) return ResponseEntity.notFound().build();

        TrainerProfile p = trainerProfileRepository.findByUserId(user.getId()).orElse(null);
        Long levelId = p != null ? LEVEL_IDS.getOrDefault(p.getTeachingLevelCode(), 1L) : 1L;

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("learnerId",  learner.getId());
        result.put("firstName",  learner.getFirstName());
        result.put("lastName",   learner.getLastName());
        result.put("email",      learner.getEmail());
        result.put("phone",      learner.getPhone());
        result.put("city",       learner.getCity());
        result.put("active",     learner.getActive());

        progressRepository.findByLearnerIdAndLevelId(learnerId, levelId).ifPresent(progress -> {
            result.put("completionPercentage", progress.getCompletionPercentage());
            result.put("coursesCompleted",     progress.getCoursesCompleted());
            result.put("sessionsAttended",     progress.getSessionsAttended());
            result.put("qcmAvgScore",          progress.getQcmAvgScore());
            result.put("totalMinutes",         progress.getTotalMinutes());
            result.put("certified",            progress.getCertified());
        });

        // Examens soumis
        List<Map<String,Object>> examResults = new ArrayList<>();
        for (Exam exam : examRepository.findByTrainerIdOrderByIdDesc(user.getId())) {
            submissionRepository.findByExamIdOrderBySubmittedAtDesc(exam.getId()).stream()
                .filter(s -> s.getLearnerId().equals(learnerId))
                .findFirst()
                .ifPresent(s -> {
                    Map<String,Object> em = new LinkedHashMap<>();
                    em.put("examTitle",   exam.getTitle());
                    em.put("grade",       s.getGrade());
                    em.put("feedback",    s.getFeedback());
                    em.put("submittedAt", s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : null);
                    examResults.add(em);
                });
        }
        result.put("examResults", examResults);
        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STATS
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/stats")
    public ResponseEntity<?> stats(Authentication auth) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        List<Course> courses = courseRepository.findAllByTrainerId(user.getId());
        TrainerProfile p = trainerProfileRepository.findByUserId(user.getId()).orElse(null);
        Long levelId = p != null ? LEVEL_IDS.getOrDefault(p.getTeachingLevelCode(), 1L) : 1L;
        long learnerCount = progressRepository.findAllByLevelIdOrderByCompletionPercentageDesc(levelId)
                .stream().filter(pr -> userRepository.findById(pr.getLearnerId()).map(u -> u.getRole() == Role.LEARNER).orElse(false)).count();
        long upcomingSessions = sessionRepository.findUpcomingByTrainerId(user.getId()).size();
        long totalExams = examRepository.findByTrainerIdOrderByIdDesc(user.getId()).size();

        return ResponseEntity.ok(Map.of(
            "totalCourses",     courses.size(),
            "publishedCourses", courses.stream().filter(c -> c.getStatus() == Course.CourseStatus.PUBLISHED).count(),
            "totalViews",       courses.stream().mapToLong(c -> c.getViewCount() != null ? c.getViewCount() : 0).sum(),
            "totalLearners",    learnerCount,
            "upcomingSessions", upcomingSessions,
            "totalExams",       totalExams,
            "teachingLevel",    p != null ? p.getTeachingLevelCode() : "?",
            "maxStudents",      p != null ? p.getMaxStudents() : 0,
            "ratingAvg",        p != null ? p.getRatingAvg() : 0.0,
            "status",           p != null ? p.getStatus().name() : "UNKNOWN"
        ));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // QCM FORMATEUR
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping("/qcms")
    public ResponseEntity<?> myQcms(Authentication auth) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        List<Map<String,Object>> result = qcmRepository.findByCreatedByOrderByIdDesc(user.getId())
            .stream().map(this::qcmToMap).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/qcms")
    @Transactional
    public ResponseEntity<?> createQcm(Authentication auth, @RequestBody Map<String, Object> body) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;

        TrainerProfile p = getApprovedProfile(user.getId());
        if (p == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error","Candidature non approuvée"));

        String title = trim(body, "title");
        if (title.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error","Titre requis"));

        Qcm qcm = new Qcm();
        qcm.setTitle(title);
        qcm.setTheme(trim(body, "theme").isEmpty() ? null : trim(body, "theme"));
        qcm.setLevelId(LEVEL_IDS.getOrDefault(p.getTeachingLevelCode(), 1L));
        qcm.setCreatedBy(user.getId());
        qcm.setStatus(Qcm.QcmStatus.DRAFT);

        @SuppressWarnings("unchecked")
        List<Map<String,Object>> questionsData = (List<Map<String,Object>>) body.getOrDefault("questions", new ArrayList<>());
        List<QcmQuestion> questions = new ArrayList<>();
        int idx = 0;
        for (Map<String,Object> qData : questionsData) {
            QcmQuestion question = new QcmQuestion();
            question.setQcm(qcm);
            question.setQuestionText((String) qData.getOrDefault("questionText", ""));
            question.setOrderIndex(idx++);
            String qt = (String) qData.getOrDefault("questionType", "SINGLE_CHOICE");
            question.setQuestionType("MULTI_CHOICE".equals(qt) ? QcmQuestion.QuestionType.MULTI_CHOICE : QcmQuestion.QuestionType.SINGLE_CHOICE);

            @SuppressWarnings("unchecked")
            List<Map<String,Object>> choicesData = (List<Map<String,Object>>) qData.getOrDefault("choices", new ArrayList<>());
            List<QcmChoice> choices = new ArrayList<>();
            for (Map<String,Object> cData : choicesData) {
                QcmChoice choice = new QcmChoice();
                choice.setQuestion(question);
                choice.setChoiceText((String) cData.getOrDefault("choiceText", ""));
                choice.setIsCorrect(Boolean.TRUE.equals(cData.get("isCorrect")));
                choice.setExplanation((String) cData.get("explanation"));
                choices.add(choice);
            }
            question.setChoices(choices);
            questions.add(question);
        }
        qcm.setQuestions(questions);
        qcmRepository.save(qcm);
        return ResponseEntity.status(HttpStatus.CREATED).body(qcmToMap(qcm));
    }

    @PostMapping("/qcms/{id}/publish")
    public ResponseEntity<?> toggleQcmPublish(Authentication auth, @PathVariable Long id) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        Qcm qcm = qcmRepository.findById(id).orElse(null);
        if (qcm == null || !user.getId().equals(qcm.getCreatedBy()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès refusé"));
        boolean wasPublished = qcm.getStatus() == Qcm.QcmStatus.PUBLISHED;
        qcm.setStatus(wasPublished ? Qcm.QcmStatus.DRAFT : Qcm.QcmStatus.PUBLISHED);
        qcmRepository.save(qcm);
        if (!wasPublished) {
            notifyLearners(user.getId(), qcm.getLevelId(), "QCM",
                "Nouveau QCM disponible",
                user.getFirstName() + " a publié un nouveau QCM : " + qcm.getTitle(), qcm.getId());
        }
        return ResponseEntity.ok(Map.of("status", qcm.getStatus().name()));
    }

    @DeleteMapping("/qcms/{id}")
    public ResponseEntity<?> deleteQcm(Authentication auth, @PathVariable Long id) {
        User user = currentUser(auth);
        ResponseEntity<?> err = forbiddenIfNotTrainer(user);
        if (err != null) return err;
        Qcm qcm = qcmRepository.findById(id).orElse(null);
        if (qcm == null || !user.getId().equals(qcm.getCreatedBy()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error","Accès refusé"));
        qcmRepository.delete(qcm);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────
    private Map<String,Object> courseToMap(Course c) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",          c.getId());
        m.put("title",       c.getTitle());
        m.put("description", c.getDescription());
        m.put("theme",       c.getTheme());
        m.put("levelId",     c.getLevelId());
        m.put("levelCode",   LEVEL_CODES.getOrDefault(c.getLevelId(),"?"));
        m.put("status",      c.getStatus().name());
        m.put("videoPath",   c.getVideoPath());
        m.put("viewCount",   c.getViewCount() != null ? c.getViewCount() : 0);
        m.put("createdAt",   c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String,Object> qcmToMap(Qcm q) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",         q.getId());
        m.put("title",      q.getTitle());
        m.put("theme",      q.getTheme());
        m.put("levelId",    q.getLevelId());
        m.put("levelCode",  LEVEL_CODES.getOrDefault(q.getLevelId(),"?"));
        m.put("status",     q.getStatus().name());
        m.put("createdBy",  q.getCreatedBy());
        int qCount = q.getQuestions() != null ? q.getQuestions().size() : 0;
        m.put("questionCount", qCount);
        return m;
    }

    private Map<String,Object> sessionToMap(StreamingSession s) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",             s.getId());
        m.put("title",          s.getTitle());
        m.put("description",    s.getDescription());
        m.put("scheduledStart", s.getScheduledStart() != null ? s.getScheduledStart().toString() : null);
        m.put("durationMinutes",s.getDurationMinutes());
        m.put("agoraChannel",   s.getAgoraChannel());
        m.put("status",         s.getStatus().name());
        m.put("levelId",        s.getLevelId());
        m.put("levelCode",      LEVEL_CODES.getOrDefault(s.getLevelId(),"?"));
        m.put("recordEnabled",  s.getRecordEnabled());
        return m;
    }
}
