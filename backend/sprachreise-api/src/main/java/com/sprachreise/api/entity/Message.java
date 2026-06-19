package com.sprachreise.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String content;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // Réponse à un message
    @Column(name = "reply_to_id")
    private Long replyToId;

    // Pièce jointe
    @Column(name = "attachment_url", columnDefinition = "TEXT")
    private String attachmentUrl;

    @Column(name = "attachment_type") // IMAGE, VIDEO, AUDIO, FILE
    private String attachmentType;

    @Column(name = "attachment_name")
    private String attachmentName;

    // Type de message
    @Column(name = "message_type") // TEXT, IMAGE, VIDEO, AUDIO, FILE
    private String messageType = "TEXT";

    // Supprimé (soft delete)
    @Column(name = "deleted")
    private Boolean deleted = false;

    // Épinglé
    @Column(name = "pinned")
    private Boolean pinned = false;

    // Modifié
    @Column(name = "edited")
    private Boolean edited = false;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) sentAt = LocalDateTime.now();
        if (messageType == null) messageType = "TEXT";
        if (deleted == null) deleted = false;
    }

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }
    public Long getSenderId()                    { return senderId; }
    public void setSenderId(Long v)              { this.senderId = v; }
    public Long getRecipientId()                 { return recipientId; }
    public void setRecipientId(Long v)           { this.recipientId = v; }
    public String getContent()                   { return content; }
    public void setContent(String v)             { this.content = v; }
    public LocalDateTime getReadAt()             { return readAt; }
    public void setReadAt(LocalDateTime v)       { this.readAt = v; }
    public LocalDateTime getSentAt()             { return sentAt; }
    public void setSentAt(LocalDateTime v)       { this.sentAt = v; }
    public Long getReplyToId()                   { return replyToId; }
    public void setReplyToId(Long v)             { this.replyToId = v; }
    public String getAttachmentUrl()             { return attachmentUrl; }
    public void setAttachmentUrl(String v)       { this.attachmentUrl = v; }
    public String getAttachmentType()            { return attachmentType; }
    public void setAttachmentType(String v)      { this.attachmentType = v; }
    public String getAttachmentName()            { return attachmentName; }
    public void setAttachmentName(String v)      { this.attachmentName = v; }
    public String getMessageType()               { return messageType; }
    public void setMessageType(String v)         { this.messageType = v; }
    public Boolean getDeleted()                  { return deleted; }
    public void setDeleted(Boolean v)            { this.deleted = v; }
    public Boolean getPinned()                   { return pinned; }
    public void setPinned(Boolean v)             { this.pinned = v; }
    public Boolean getEdited()                   { return edited; }
    public void setEdited(Boolean v)             { this.edited = v; }
}
