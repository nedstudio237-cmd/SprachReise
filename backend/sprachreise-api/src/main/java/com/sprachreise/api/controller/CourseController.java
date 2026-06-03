package com.sprachreise.api.controller;

import com.sprachreise.api.dto.CourseDto;
import com.sprachreise.api.entity.Course;
import com.sprachreise.api.entity.User;
import com.sprachreise.api.repository.CourseRepository;
import com.sprachreise.api.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final Map<Long, String> LEVEL_CODES = Map.of(
        1L, "A1", 2L, "A2", 3L, "B1", 4L, "B2", 5L, "C1", 6L, "C2"
    );

    private final CourseRepository courseRepository;
    private final UserRepository   userRepository;

    public CourseController(CourseRepository courseRepository,
                            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository   = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<CourseDto>> getAll(@RequestParam(required = false) Long levelId) {
        List<Course> courses = levelId != null
            ? courseRepository.findByLevelIdPublished(levelId)
            : courseRepository.findAllPublished();
        return ResponseEntity.ok(courses.stream()
            .map(c -> CourseDto.from(c, LEVEL_CODES.getOrDefault(c.getLevelId(), "?")))
            .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return courseRepository.findById(id)
            .map(c -> ResponseEntity.ok(CourseDto.from(c, LEVEL_CODES.getOrDefault(c.getLevelId(), "?"))))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getStats(@PathVariable Long id) {
        return courseRepository.findById(id)
            .map(c -> ResponseEntity.ok(Map.of(
                "views", c.getViewCount() == null ? 0 : c.getViewCount(),
                "completionRate", 0.0,
                "ratingAvg", 0.0)))
            .orElse(ResponseEntity.notFound().build());
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object p = auth.getPrincipal();
        if (!(p instanceof UserDetails)) return null;
        return userRepository.findByEmail(((UserDetails) p).getUsername()).orElse(null);
    }
}
