package com.sprachreise.api.dto;

import com.sprachreise.api.entity.Message;

public class MessageDto {
    public Long id;
    public Long senderId;
    public Long recipientId;
    public String content;
    public String readAt;
    public String sentAt;

    public static MessageDto from(Message m) {
        MessageDto dto = new MessageDto();
        dto.id = m.getId();
        dto.senderId = m.getSenderId();
        dto.recipientId = m.getRecipientId();
        dto.content = m.getContent();
        dto.readAt = m.getReadAt() != null ? m.getReadAt().toString() : null;
        dto.sentAt = m.getSentAt() != null ? m.getSentAt().toString() : null;
        return dto;
    }
}
