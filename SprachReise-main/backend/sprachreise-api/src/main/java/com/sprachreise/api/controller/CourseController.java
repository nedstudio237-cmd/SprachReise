package com.sprachreise.api.controller;

import com.sprachreise.api.dto.CourseDto;
import com.sprachreise.api.entity.Course;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.CourseRepository;
import com.sprachreise.api.repository.TrainerProfileRepository;
import com.sprachreise.api.repository.UserRepository;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L, "A1", 2L, "A2", 3L, "B1", 4L, "B2", 5L, "C1", 6L, "C2"
    );

    private static final Set<String> ALLOWED_VIDEO_EXT = Set.of("mp4", "mov");
    private static final Set<String> ALLOWED_PDF_EXT = Set.of("pdf");
    private static final long MAX_VIDEO_BYTES = 500L * 1024 * 1024;
    private static final long MAX_PDF_BYTES = 20L * 1024 * 1024;

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final TrainerProfileRepository trainerProfileRepository;

    @Value("${storage.upload-dir}")
    private String storageDir;

    public CourseController(CourseRepository courseRepository,
                            UserRepository userRepository,
                            TrainerProfileRepository trainerProfileRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.trainerProfileRepository = trainerProfileRepository;
    }

    @GetMapping
    public ResponseEntity<List<CourseDto>> getAll(@RequestParam(required = false) Long levelId) {
        List<Course> courses = levelId != null
            ? courseRepository.findByLevelIdPublished(levelId)
            : courseRepository.findAllPublished();

        List<CourseDto> dtos = courses.stream()
            .map(c -> CourseDto.from(c, LEVEL_CODES.getOrDefault(c.getLevelId(), "?")))
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> getMine() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        List<CourseDto> dtos = courseRepository.findAllByTrainerId(me.getId()).stream()
            .map(c -> CourseDto.from(c, LEVEL_CODES.getOrDefault(c.getLevelId(), "?")))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return courseRepository.findById(id)
            .map(c -> ResponseEntity.ok(CourseDto.from(c, LEVEL_CODES.getOrDefault(c.getLevelId(), "?"))))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getStats(@PathVariable Long id) {
        Optional<Course> opt = courseRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Course c = opt.get();
        // TODO: completionRate and ratingAvg not tracked yet — placeholders.
        return ResponseEntity.ok(Map.of(
            "views", c.getViewCount() == null ? 0 : c.getViewCount(),
            "completionRate", 0.0,
            "ratingAvg", 0.0
        ));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<?> create(
        @RequestParam("title") String title,
        @RequestParam("description") String description,
        @RequestParam(value = "theme", required = false) String theme,
        @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
        @RequestParam("pdfFile") MultipartFile pdfFile,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "publishAt", required = false) String publishAt
    ) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }

        Optional<TrainerProfile> tpOpt = trainerProfileRepository.findByUserId(me.getId());
        if (tpOpt.isEmpty() || tpOpt.get().getAssignedLevelId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Profil formateur sans niveau assigné"));
        }
        Long lockedLevelId = tpOpt.get().getAssignedLevelId();

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Titre requis"));
        }
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Description requise"));
        }
        if (pdfFile == null || pdfFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "PDF requis"));
        }

        // PDF validation
        String pdfExt = extOf(pdfFile.getOriginalFilename());
        if (!ALLOWED_PDF_EXT.contains(pdfExt)) {
            return ResponseEntity.badRequest().body(Map.of("error", "PDF : format .pdf requis"));
        }
        if (pdfFile.getSize() > MAX_PDF_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "PDF > 20 Mo"));
        }

        // Video validation (optional)
        String videoExt = null;
        if (videoFile != null && !videoFile.isEmpty()) {
            videoExt = extOf(videoFile.getOriginalFilename());
            if (!ALLOWED_VIDEO_EXT.contains(videoExt)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vidéo : format .mp4 ou .mov requis"));
            }
            if (videoFile.getSize() > MAX_VIDEO_BYTES) {
                return ResponseEntity.badRequest().body(Map.of("error", "Vidéo > 500 Mo"));
            }
        }

        Course.CourseStatus parsedStatus = Course.CourseStatus.DRAFT;
        if (status != null && !status.isBlank()) {
            try {
                Course.CourseStatus s = Course.CourseStatus.valueOf(status.trim().toUpperCase());
                if (s == Course.CourseStatus.REMOVED) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Statut invalide"));
                }
                parsedStatus = s;
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Statut invalide"));
            }
        }

        LocalDateTime parsedPublishAt = null;
        if (publishAt != null && !publishAt.isBlank()) {
            try {
                parsedPublishAt = LocalDateTime.parse(publishAt.trim());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "publishAt invalide (ISO attendu)"));
            }
        }

        try {
            long ts = System.currentTimeMillis();

            // Save PDF
            Path pdfDir = Paths.get(storageDir, "courses", "pdf");
            Files.createDirectories(pdfDir);
            String pdfFilename = "course_" + me.getId() + "_" + ts + ".pdf";
            Path pdfTarget = pdfDir.resolve(pdfFilename);
            Files.copy(pdfFile.getInputStream(), pdfTarget, StandardCopyOption.REPLACE_EXISTING);
            String pdfRelative = "courses/pdf/" + pdfFilename;

            // Save Video (if present)
            String videoRelative = null;
            if (videoFile != null && !videoFile.isEmpty()) {
                Path videoDir = Paths.get(storageDir, "courses", "videos");
                Files.createDirectories(videoDir);
                String videoFilename = "course_" + me.getId() + "_" + ts + "." + videoExt;
                Path videoTarget = videoDir.resolve(videoFilename);
                Files.copy(videoFile.getInputStream(), videoTarget, StandardCopyOption.REPLACE_EXISTING);
                videoRelative = "courses/videos/" + videoFilename;
            }

            Course course = new Course();
            course.setTrainer(me);
            course.setLevelId(lockedLevelId);
            course.setTitle(title.trim());
            course.setDescription(description.trim());
            course.setTheme(theme == null ? null : theme.trim());
            course.setPdfPath(pdfRelative);
            course.setPdfSizeBytes(pdfFile.getSize());
            course.setVideoPath(videoRelative);
            course.setStatus(parsedStatus);
            course.setPublishAt(parsedPublishAt);

            Course saved = courseRepository.save(course);
            CourseDto dto = CourseDto.from(saved, LEVEL_CODES.getOrDefault(saved.getLevelId(), "?"));
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Échec de l'upload : " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }

        Optional<Course> opt = courseRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Course course = opt.get();

        if (course.getTrainer() == null || !course.getTrainer().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas propriétaire de ce cours"));
        }

        if (body.containsKey("title")) {
            String v = strOf(body.get("title"));
            if (v.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Titre requis"));
            course.setTitle(v);
        }
        if (body.containsKey("description")) {
            String v = strOf(body.get("description"));
            if (v.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Description requise"));
            course.setDescription(v);
        }
        if (body.containsKey("theme")) {
            course.setTheme(strOf(body.get("theme")));
        }
        if (body.containsKey("status")) {
            String s = strOf(body.get("status")).toUpperCase();
            try {
                Course.CourseStatus ns = Course.CourseStatus.valueOf(s);
                if (ns == Course.CourseStatus.REMOVED) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Utilisez DELETE pour supprimer"));
                }
                course.setStatus(ns);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Statut invalide"));
            }
        }
        if (body.containsKey("publishAt")) {
            Object v = body.get("publishAt");
            if (v == null || strOf(v).isEmpty()) {
                course.setPublishAt(null);
            } else {
                try {
                    course.setPublishAt(LocalDateTime.parse(strOf(v)));
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "publishAt invalide (ISO attendu)"));
                }
            }
        }

        Course saved = courseRepository.save(course);
        return ResponseEntity.ok(CourseDto.from(saved, LEVEL_CODES.getOrDefault(saved.getLevelId(), "?")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }

        Optional<Course> opt = courseRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Course course = opt.get();

        if (course.getTrainer() == null || !course.getTrainer().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Vous n'êtes pas propriétaire de ce cours"));
        }
        course.setStatus(Course.CourseStatus.REMOVED);
        courseRepository.save(course);
        return ResponseEntity.ok(Map.of("status", "REMOVED"));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) return null;
        String email = ((UserDetails) principal).getUsername();
        return userRepository.findByEmail(email).orElse(null);
    }

    private static String extOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "";
        return filename.substring(dot + 1).toLowerCase();
    }

    private static String strOf(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
