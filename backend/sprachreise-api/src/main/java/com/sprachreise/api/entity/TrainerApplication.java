package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trainer_applications")
public class TrainerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "email")
    private String email;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "native_language")
    private String nativeLanguage;

    @Column(name = "requested_level_id", nullable = false)
    private Long requestedLevelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "diploma_pdf_path", nullable = false, columnDefinition = "TEXT")
    private String diplomaPdfPath;

    @Column(name = "motivation", columnDefinition = "TEXT")
    private String motivation;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_motif", columnDefinition = "TEXT")
    private String reviewMotif;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }

    public enum Status { PENDING, APPROVED, REJECTED, SUSPENDED }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getNativeLanguage() { return nativeLanguage; }
    public void setNativeLanguage(String nativeLanguage) { this.nativeLanguage = nativeLanguage; }

    public Long getRequestedLevelId() { return requestedLevelId; }
    public void setRequestedLevelId(Long requestedLevelId) { this.requestedLevelId = requestedLevelId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getDiplomaPdfPath() { return diplomaPdfPath; }
    public void setDiplomaPdfPath(String diplomaPdfPath) { this.diplomaPdfPath = diplomaPdfPath; }

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }

    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewMotif() { return reviewMotif; }
    public void setReviewMotif(String reviewMotif) { this.reviewMotif = reviewMotif; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
