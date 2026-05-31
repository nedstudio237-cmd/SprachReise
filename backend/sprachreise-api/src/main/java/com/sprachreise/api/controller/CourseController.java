package com.sprachreise.api.controller;

import com.sprachreise.api.dto.CourseDto;
import com.sprachreise.api.entity.Course;
import com.sprachreise.api.repository.CourseRepository;
import org.springframework.http.ResponseEntity;
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

    public CourseController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return courseRepository.findById(id)
            .map(c -> ResponseEntity.ok(CourseDto.from(c, LEVEL_CODES.getOrDefault(c.getLevelId(), "?"))))
            .orElse(ResponseEntity.notFound().build());
    }
}
