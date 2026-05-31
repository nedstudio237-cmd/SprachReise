package com.sprachreise.api.dto;

import com.sprachreise.api.entity.StreamingSession;

public class SessionDto {
    public Long id;
    public Long levelId;
    public String title;
    public String description;
    public String scheduledStart;
    public Integer durationMinutes;
    public String status;
    public String trainerName;
    public String agoraChannel;

    public static SessionDto from(StreamingSession s) {
        SessionDto dto = new SessionDto();
        dto.id = s.getId();
        dto.levelId = s.getLevelId();
        dto.title = s.getTitle();
        dto.description = s.getDescription();
        dto.scheduledStart = s.getScheduledStart() != null ? s.getScheduledStart().toString() : null;
        dto.durationMinutes = s.getDurationMinutes();
        dto.status = s.getStatus().name();
        dto.agoraChannel = s.getAgoraChannel();
        if (s.getTrainer() != null) {
            dto.trainerName = s.getTrainer().getFirstName() + " " + s.getTrainer().getLastName();
        }
        return dto;
    }
}
