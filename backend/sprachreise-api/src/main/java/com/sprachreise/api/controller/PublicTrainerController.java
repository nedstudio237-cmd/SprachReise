package com.sprachreise.api.controller;

import com.sprachreise.api.entity.*;
import com.sprachreise.api.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/trainers")
public class PublicTrainerController {

    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L,"A1",2L,"A2",3L,"B1",4L,"B2",5L,"C1",6L,"C2"
    );

    private final UserRepository           userRepository;
    private final TrainerProfileRepository profileRepository;
    private final CourseRepository         courseRepository;
    private final QcmRepository            qcmRepository;
    private final ExamRepository           examRepository;

    public PublicTrainerController(UserRepository userRepository,
                                   TrainerProfileRepository profileRepository,
                                   CourseRepository courseRepository,
                                   QcmRepository qcmRepository,
                                   ExamRepository examRepository) {
        this.userRepository    = userRepository;
        this.profileRepository = profileRepository;
        this.courseRepository  = courseRepository;
        this.qcmRepository     = qcmRepository;
        this.examRepository    = examRepository;
    }

    /** Liste tous les formateurs approuvés */
    @GetMapping
    public ResponseEntity<?> listTrainers(@RequestParam(required = false) String level) {
        List<Map<String,Object>> result = new ArrayList<>();
        List<TrainerProfile> profiles = profileRepository.findAll().stream()
            .filter(p -> p.getStatus() == TrainerProfile.Status.APPROVED)
            .filter(p -> level == null || level.equals(p.getTeachingLevelCode()))
            .toList();
        for (TrainerProfile p : profiles) {
            userRepository.findById(p.getUserId()).ifPresent(u -> result.add(buildProfile(u, p)));
        }
        return ResponseEntity.ok(result);
    }

    /** Profil public d'un formateur */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTrainer(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null || user.getRole() != Role.TRAINER) return ResponseEntity.notFound().build();
        TrainerProfile p = profileRepository.findByUserId(id).orElse(null);
        if (p == null || p.getStatus() != TrainerProfile.Status.APPROVED)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(buildProfile(user, p));
    }

    /** Cours publiés d'un formateur */
    @GetMapping("/{id}/courses")
    public ResponseEntity<?> getTrainerCourses(@PathVariable Long id) {
        List<Map<String,Object>> result = courseRepository.findAllByTrainerId(id).stream()
            .filter(c -> c.getStatus() == Course.CourseStatus.PUBLISHED)
            .map(c -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id",          c.getId());
                m.put("title",       c.getTitle());
                m.put("description", c.getDescription());
                m.put("theme",       c.getTheme());
                m.put("levelCode",   LEVEL_CODES.getOrDefault(c.getLevelId(),"?"));
                m.put("videoPath",   c.getVideoPath());
                m.put("pdfPath",     c.getPdfPath());
                m.put("viewCount",   c.getViewCount() != null ? c.getViewCount() : 0);
                return m;
            }).toList();
        return ResponseEntity.ok(result);
    }

    /** QCMs publiés d'un formateur */
    @GetMapping("/{id}/qcms")
    public ResponseEntity<?> getTrainerQcms(@PathVariable Long id) {
        List<Map<String,Object>> result = qcmRepository.findByCreatedByOrderByIdDesc(id).stream()
            .filter(q -> q.getStatus() == Qcm.QcmStatus.PUBLISHED)
            .map(q -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id",            q.getId());
                m.put("title",         q.getTitle());
                m.put("theme",         q.getTheme());
                m.put("levelCode",     LEVEL_CODES.getOrDefault(q.getLevelId(),"?"));
                m.put("questionCount", q.getQuestions() != null ? q.getQuestions().size() : 0);
                return m;
            }).toList();
        return ResponseEntity.ok(result);
    }

    /** Évaluations publiées d'un formateur */
    @GetMapping("/{id}/exams")
    public ResponseEntity<?> getTrainerExams(@PathVariable Long id) {
        List<Map<String,Object>> result = examRepository.findByTrainerIdOrderByIdDesc(id).stream()
            .filter(e -> e.getStatus() == Exam.ExamStatus.PUBLISHED)
            .map(e -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id",           e.getId());
                m.put("title",        e.getTitle());
                m.put("instructions", e.getInstructions());
                m.put("scheduledAt",  e.getScheduledAt() != null ? e.getScheduledAt().toString() : null);
                return m;
            }).toList();
        return ResponseEntity.ok(result);
    }

    private Map<String,Object> buildProfile(User u, TrainerProfile p) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",             u.getId());
        m.put("firstName",      u.getFirstName());
        m.put("lastName",       u.getLastName());
        m.put("bio",            u.getBio());
        m.put("photoUrl",       u.getPhotoUrl());
        m.put("teachingLevel",  p.getTeachingLevelCode());
        m.put("maxStudents",    p.getMaxStudents());
        m.put("currentStudents",p.getCurrentStudents());
        m.put("ratingAvg",      p.getRatingAvg());
        return m;
    }
}
