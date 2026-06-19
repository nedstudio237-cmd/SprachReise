package com.sprachreise.api.dto;

import com.sprachreise.api.entity.Message;

public class MessageDto {
    public Long   id;
    public Long   senderId;
    public Long   recipientId;
    public String content;
    public String readAt;
    public String sentAt;
    public Long   replyToId;
    public String replyToContent; // aperçu du message cité
    public String attachmentUrl;
    public String attachmentType;
    public String attachmentName;
    public String messageType;
    public Boolean deleted;
    public Boolean pinned;
    public Boolean edited;

    public static MessageDto from(Message m) {
        MessageDto dto = new MessageDto();
        dto.id             = m.getId();
        dto.senderId       = m.getSenderId();
        dto.recipientId    = m.getRecipientId();
        dto.content        = Boolean.TRUE.equals(m.getDeleted()) ? null : m.getContent();
        dto.readAt         = m.getReadAt()  != null ? m.getReadAt().toString()  : null;
        dto.sentAt         = m.getSentAt()  != null ? m.getSentAt().toString()  : null;
        dto.replyToId      = m.getReplyToId();
        dto.attachmentUrl  = m.getAttachmentUrl();
        dto.attachmentType = m.getAttachmentType();
        dto.attachmentName = m.getAttachmentName();
        dto.messageType    = m.getMessageType() != null ? m.getMessageType() : "TEXT";
        dto.deleted        = m.getDeleted();
        dto.pinned         = m.getPinned();
        dto.edited         = m.getEdited();
        return dto;
    }
}
