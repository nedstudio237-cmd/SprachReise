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

    @Column(name = "recording_path", columnDefinition = "TEXT")
    private String recordingPath;

    @Column(name = "attachment_pdf", columnDefinition = "TEXT")
    private String attachmentPdf;

    @Column(name = "record_enabled")
    private Boolean recordEnabled = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum SessionStatus { SCHEDULED, LIVE, ENDED, CANCELLED }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getTrainer() { return trainer; }
    public void setTrainer(User trainer) { this.trainer = trainer; }

    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(LocalDateTime scheduledStart) { this.scheduledStart = scheduledStart; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getAgoraChannel() { return agoraChannel; }
    public void setAgoraChannel(String agoraChannel) { this.agoraChannel = agoraChannel; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public String getRecordingPath() { return recordingPath; }
    public void setRecordingPath(String recordingPath) { this.recordingPath = recordingPath; }

    public String getAttachmentPdf() { return attachmentPdf; }
    public void setAttachmentPdf(String attachmentPdf) { this.attachmentPdf = attachmentPdf; }

    public Boolean getRecordEnabled() { return recordEnabled; }
    public void setRecordEnabled(Boolean recordEnabled) { this.recordEnabled = recordEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
