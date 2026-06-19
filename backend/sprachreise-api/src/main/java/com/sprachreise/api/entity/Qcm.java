package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "qcms")
public class Qcm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_id")
    private Long levelId;

    private String title;
    private String theme;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private QcmStatus status = QcmStatus.DRAFT;

    @OneToMany(mappedBy = "qcm", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QcmQuestion> questions;

    public enum QcmStatus { DRAFT, PUBLISHED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public QcmStatus getStatus() { return status; }
    public void setStatus(QcmStatus status) { this.status = status; }

    public List<QcmQuestion> getQuestions() { return questions; }
    public void setQuestions(List<QcmQuestion> questions) { this.questions = questions; }
}
