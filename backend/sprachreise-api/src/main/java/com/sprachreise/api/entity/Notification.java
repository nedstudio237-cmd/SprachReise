package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type; // COURSE, SESSION, QCM, EXAM, MESSAGE

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId()            { return id; }
    public void setId(Long id)     { this.id = id; }

    public Long getUserId()                   { return userId; }
    public void setUserId(Long userId)        { this.userId = userId; }

    public String getType()                   { return type; }
    public void setType(String type)          { this.type = type; }

    public String getTitle()                  { return title; }
    public void setTitle(String title)        { this.title = title; }

    public String getBody()                   { return body; }
    public void setBody(String body)          { this.body = body; }

    public Long getEntityId()                 { return entityId; }
    public void setEntityId(Long entityId)    { this.entityId = entityId; }

    public Boolean getIsRead()                { return isRead; }
    public void setIsRead(Boolean isRead)     { this.isRead = isRead; }

    public LocalDateTime getCreatedAt()       { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
