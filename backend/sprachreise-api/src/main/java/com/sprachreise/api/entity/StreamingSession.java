package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "streaming_sessions")
public class StreamingSession {
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

    @Column(name = "scheduled_start")
    private LocalDateTime scheduledStart;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "agora_channel")
    private String agoraChannel;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum SessionStatus { SCHEDULED, LIVE, ENDED, CANCELLED }

    public Long getId() { return id; }
    public User getTrainer() { return trainer; }
    public Long getLevelId() { return levelId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getAgoraChannel() { return agoraChannel; }
    public SessionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
