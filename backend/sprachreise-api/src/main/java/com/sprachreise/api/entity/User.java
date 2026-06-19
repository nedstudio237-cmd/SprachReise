package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.LEARNER;

    @Column(name = "photo_url")
    private String photoUrl;

    private String city;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    private String bio;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "level_code")
    private String levelCode = "A1"; // CECRL level for learner

    @Column(name = "assigned_trainer_id")
    private Long assignedTrainerId;

    // ── Abonnement ────────────────────────────────────────────────────────────
    @Column(name = "subscription_plan")
    private String subscriptionPlan = "TRIAL"; // TRIAL, BASIC, STANDARD, PREMIUM, EXPIRED

    @Column(name = "subscription_status")
    private String subscriptionStatus = "TRIAL"; // TRIAL, ACTIVE, EXPIRED, CANCELLED

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "subscription_starts_at")
    private LocalDateTime subscriptionStartsAt;

    @Column(name = "subscription_ends_at")
    private LocalDateTime subscriptionEndsAt;

    @Column(name = "reminder_5d_sent")
    private Boolean reminder5dSent = false;

    @Column(name = "reminder_0d_sent")
    private Boolean reminder0dSent = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }

    public Long getAssignedTrainerId() { return assignedTrainerId; }
    public void setAssignedTrainerId(Long assignedTrainerId) { this.assignedTrainerId = assignedTrainerId; }

    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public LocalDateTime getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(LocalDateTime trialEndsAt) { this.trialEndsAt = trialEndsAt; }

    public LocalDateTime getSubscriptionStartsAt() { return subscriptionStartsAt; }
    public void setSubscriptionStartsAt(LocalDateTime v) { this.subscriptionStartsAt = v; }

    public LocalDateTime getSubscriptionEndsAt() { return subscriptionEndsAt; }
    public void setSubscriptionEndsAt(LocalDateTime v) { this.subscriptionEndsAt = v; }

    public Boolean getReminder5dSent() { return reminder5dSent; }
    public void setReminder5dSent(Boolean v) { this.reminder5dSent = v; }

    public Boolean getReminder0dSent() { return reminder0dSent; }
    public void setReminder0dSent(Boolean v) { this.reminder0dSent = v; }
}
