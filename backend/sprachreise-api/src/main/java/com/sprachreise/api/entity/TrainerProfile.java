package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainer_profiles")
public class TrainerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "native_language")
    private String nativeLanguage;

    @Column(name = "teaching_level_code")
    private String teachingLevelCode; // A1, A2, B1, B2, C1, C2

    @Column(name = "teaching_language_code")
    private String teachingLanguageCode = "de";

    @Column(name = "max_students")
    private Integer maxStudents = 30;

    @Column(name = "current_students")
    private Integer currentStudents = 0;

    @Column(name = "rating_avg")
    private Double ratingAvg = 0.0;

    @Column(columnDefinition = "TEXT")
    private String motivation;

    @Column(name = "diploma_pdf_path")
    private String diplomaPdfPath;

    public enum Status { PENDING, APPROVED, REJECTED }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_motif")
    private String reviewMotif;

    @PrePersist
    protected void onCreate() { submittedAt = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId()                          { return id; }
    public Long getUserId()                      { return userId; }
    public void setUserId(Long v)               { this.userId = v; }
    public String getNativeLanguage()            { return nativeLanguage; }
    public void setNativeLanguage(String v)     { this.nativeLanguage = v; }
    public String getTeachingLevelCode()         { return teachingLevelCode; }
    public void setTeachingLevelCode(String v)  { this.teachingLevelCode = v; }
    public String getTeachingLanguageCode()      { return teachingLanguageCode; }
    public void setTeachingLanguageCode(String v){ this.teachingLanguageCode = v; }
    public Integer getMaxStudents()              { return maxStudents; }
    public void setMaxStudents(Integer v)       { this.maxStudents = v; }
    public Double getRatingAvg()                 { return ratingAvg; }
    public void setRatingAvg(Double v)          { this.ratingAvg = v; }
    public Status getStatus()                    { return status; }
    public void setStatus(Status v)             { this.status = v; }
    public LocalDateTime getSubmittedAt()        { return submittedAt; }
    public LocalDateTime getReviewedAt()         { return reviewedAt; }
    public void setReviewedAt(LocalDateTime v)  { this.reviewedAt = v; }
    public String getReviewMotif()               { return reviewMotif; }
    public void setReviewMotif(String v)        { this.reviewMotif = v; }
    public String getMotivation()                { return motivation; }
    public void setMotivation(String v)         { this.motivation = v; }
    public String getDiplomaPdfPath()            { return diplomaPdfPath; }
    public void setDiplomaPdfPath(String v)     { this.diplomaPdfPath = v; }

    public Integer getCurrentStudents()          { return currentStudents != null ? currentStudents : 0; }
    public void setCurrentStudents(Integer v)   { this.currentStudents = v; }
}
