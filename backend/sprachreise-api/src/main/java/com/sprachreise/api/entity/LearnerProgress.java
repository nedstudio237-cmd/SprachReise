package com.sprachreise.api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "learner_progress")
public class LearnerProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learner_id", nullable = false)
    private Long learnerId;

    @Column(name = "level_id", nullable = false)
    private Long levelId;

    @Column(name = "courses_completed")
    private Integer coursesCompleted = 0;

    @Column(name = "sessions_attended")
    private Integer sessionsAttended = 0;

    @Column(name = "qcm_avg_score", precision = 5, scale = 2)
    private BigDecimal qcmAvgScore = BigDecimal.ZERO;

    @Column(name = "total_minutes")
    private Integer totalMinutes = 0;

    @Column(name = "completion_percentage", precision = 5, scale = 2)
    private BigDecimal completionPercentage = BigDecimal.ZERO;

    @Column(name = "certified")
    private Boolean certified = false;

    @Column(name = "certified_at")
    private LocalDateTime certifiedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLearnerId() { return learnerId; }
    public void setLearnerId(Long learnerId) { this.learnerId = learnerId; }

    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }

    public Integer getCoursesCompleted() { return coursesCompleted; }
    public void setCoursesCompleted(Integer coursesCompleted) { this.coursesCompleted = coursesCompleted; }

    public Integer getSessionsAttended() { return sessionsAttended; }
    public void setSessionsAttended(Integer sessionsAttended) { this.sessionsAttended = sessionsAttended; }

    public BigDecimal getQcmAvgScore() { return qcmAvgScore; }
    public void setQcmAvgScore(BigDecimal qcmAvgScore) { this.qcmAvgScore = qcmAvgScore; }

    public Integer getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(Integer totalMinutes) { this.totalMinutes = totalMinutes; }

    public BigDecimal getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(BigDecimal completionPercentage) { this.completionPercentage = completionPercentage; }

    public Boolean getCertified() { return certified; }
    public void setCertified(Boolean certified) { this.certified = certified; }

    public LocalDateTime getCertifiedAt() { return certifiedAt; }
    public void setCertifiedAt(LocalDateTime certifiedAt) { this.certifiedAt = certifiedAt; }
}
