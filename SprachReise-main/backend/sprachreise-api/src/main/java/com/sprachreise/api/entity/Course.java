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

    @Column(name = "pdf_size_bytes")
    private Long pdfSizeBytes;

    @Enumerated(EnumType.STRING)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "publish_at")
    private LocalDateTime publishAt;

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
    public Long getPdfSizeBytes() { return pdfSizeBytes; }
    public CourseStatus getStatus() { return status; }
    public Integer getViewCount() { return viewCount; }
    public LocalDateTime getPublishAt() { return publishAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setTrainer(User trainer) { this.trainer = trainer; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setTheme(String theme) { this.theme = theme; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
    public void setVideoDurationSec(Integer videoDurationSec) { this.videoDurationSec = videoDurationSec; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public void setPdfSizeBytes(Long pdfSizeBytes) { this.pdfSizeBytes = pdfSizeBytes; }
    public void setStatus(CourseStatus status) { this.status = status; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public void setPublishAt(LocalDateTime publishAt) { this.publishAt = publishAt; }
}
