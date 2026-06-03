package com.sprachreise.api.controller;

import com.sprachreise.api.dto.CourseDto;
import com.sprachreise.api.dto.SessionDto;
import com.sprachreise.api.dto.TrainerDetailDto;
import com.sprachreise.api.dto.TrainerDto;
import com.sprachreise.api.entity.Role;
import com.sprachreise.api.entity.TrainerProfile;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.CourseRepository;
import com.sprachreise.api.repository.StreamingSessionRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L, "A1", 2L, "A2", 3L, "B1", 4L, "B2", 5L, "C1", 6L, "C2"
    );

    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    private static final int BIO_MAX_LENGTH = 1000;

    private final TrainerProfileRepository trainerProfileRepository;
    private final CourseRepository courseRepository;
    private final StreamingSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Value("${storage.upload-dir}")
    private String storageDir;

    public TrainerController(TrainerProfileRepository trainerProfileRepository,
                             CourseRepository courseRepository,
                             StreamingSessionRepository sessionRepository,
                             UserRepository userRepository) {
        this.trainerProfileRepository = trainerProfileRepository;
        this.courseRepository = courseRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<TrainerDto>> getAll() {
        List<TrainerDto> dtos = trainerProfileRepository.findAllActive().stream()
            .map(tp -> TrainerDto.from(tp, LEVEL_CODES.getOrDefault(tp.getAssignedLevelId(), "?")))
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        return buildDetail(me.getId());
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody Map<String, String> body) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }

        if (body.containsKey("firstName")) {
            String v = trim(body.get("firstName"));
            if (v.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Prénom requis"));
            me.setFirstName(v);
        }
        if (body.containsKey("lastName")) {
            String v = trim(body.get("lastName"));
            if (v.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Nom requis"));
            me.setLastName(v);
        }
        if (body.containsKey("phone")) me.setPhone(trim(body.get("phone")));
        if (body.containsKey("city")) me.setCity(trim(body.get("city")));
        if (body.containsKey("bio")) {
            String v = trim(body.get("bio"));
            if (v.length() > BIO_MAX_LENGTH) {
                return ResponseEntity.badRequest().body(Map.of("error", "Biographie : " + BIO_MAX_LENGTH + " caractères max"));
            }
            me.setBio(v);
        }
        userRepository.save(me);
        return buildDetail(me.getId());
    }

    @PostMapping("/me/photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file) {
        User me = currentUser();
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (me.getRole() != Role.TRAINER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Réservé aux formateurs"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            return ResponseEntity.badRequest().body(Map.of("error", "Photo > 5 Mo"));
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot + 1).toLowerCase();
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Format accepté : jpg, png, webp"));
        }

        try {
            Path photosDir = Paths.get(storageDir, "photos");
            Files.createDirectories(photosDir);
            String filename = "trainer_" + me.getId() + "_" + System.currentTimeMillis() + "." + ext;
            Path target = photosDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String relative = "photos/" + filename;
            me.setPhotoUrl(relative);
            userRepository.save(me);
            return ResponseEntity.ok(Map.of("photoUrl", relative));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Échec de l'upload : " + e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getByUserId(@PathVariable Long userId) {
        return buildDetail(userId);
    }

    private ResponseEntity<?> buildDetail(Long userId) {
        return trainerProfileRepository.findByUserId(userId)
            .map(tp -> {
                TrainerDetailDto detail = new TrainerDetailDto();
                detail.profile = TrainerDto.from(tp, LEVEL_CODES.getOrDefault(tp.getAssignedLevelId(), "?"));
                detail.publishedCourses = courseRepository.findByTrainerIdPublished(userId).stream()
                    .map(c -> CourseDto.from(c, LEVEL_CODES.getOrDefault(c.getLevelId(), "?")))
                    .collect(Collectors.toList());
                detail.upcomingSessions = sessionRepository.findUpcomingByTrainerId(userId).stream()
                    .map(SessionDto::from)
                    .collect(Collectors.toList());
                return ResponseEntity.ok((Object) detail);
            })
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

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
