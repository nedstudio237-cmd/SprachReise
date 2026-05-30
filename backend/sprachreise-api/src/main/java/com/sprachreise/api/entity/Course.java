package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private User trainer;

    @Column(name = "level_id")
    private Long levelId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String theme;

    @Column(name = "video_path")
    private String videoPath;

    @Column(name = "video_duration_sec")
    private Integer videoDurationSec;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Enumerated(EnumType.STRING)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum CourseStatus { DRAFT, PUBLISHED, REMOVED }

    public Long getId() { return id; }
    public User getTrainer() { return trainer; }
    public Long getLevelId() { return levelId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTheme() { return theme; }
    public String getVideoPath() { return videoPath; }
    public Integer getVideoDurationSec() { return videoDurationSec; }
    public String getPdfPath() { return pdfPath; }
    public CourseStatus getStatus() { return status; }
    public Integer getViewCount() { return viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
