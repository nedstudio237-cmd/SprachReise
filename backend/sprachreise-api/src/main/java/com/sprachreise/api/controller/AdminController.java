package com.sprachreise.api.controller;

import com.sprachreise.api.entity.Course;
import com.sprachreise.api.repository.CourseRepository;
import com.sprachreise.api.service.PdfGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CourseRepository courseRepository;
    private final PdfGeneratorService pdfGeneratorService;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminController(CourseRepository courseRepository, PdfGeneratorService pdfGeneratorService) {
        this.courseRepository = courseRepository;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @PostMapping("/generate-course-pdfs")
    @Transactional
    public ResponseEntity<?> generateAllCoursePdfs() {
        List<Map<String, Object>> results = new ArrayList<>();
        int success = 0, failed = 0;

        List<Course> courses = courseRepository.findAll();
        for (Course course : courses) {
            try {
                String pdfPath = pdfGeneratorService.generateCoursePdf(course);
                entityManager.createNativeQuery(
                        "UPDATE courses SET pdf_path = ? WHERE id = ?")
                        .setParameter(1, pdfPath)
                        .setParameter(2, course.getId())
                        .executeUpdate();
                results.add(Map.of("id", course.getId(), "title", course.getTitle(),
                        "path", pdfPath, "status", "ok"));
                success++;
            } catch (Exception e) {
                results.add(Map.of("id", course.getId(), "title", course.getTitle(),
                        "error", e.getMessage(), "status", "error"));
                failed++;
            }
        }
        return ResponseEntity.ok(Map.of(
                "generated", success, "failed", failed,
                "total", courses.size(), "details", results));
    }
}
