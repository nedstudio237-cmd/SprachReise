package com.sprachreise.api.dto;

import com.sprachreise.api.entity.Exam;

import java.time.LocalDateTime;

public class ExamDto {
    public Long id;
    public Long trainerId;
    public Long levelId;
    public String levelCode;
    public String title;
    public String instructions;
    public LocalDateTime scheduledAt;
    public String status;
    public LocalDateTime createdAt;

    public static ExamDto from(Exam exam, String levelCode) {
        ExamDto dto = new ExamDto();
        dto.id = exam.getId();
        dto.trainerId = exam.getTrainerId();
        dto.levelId = exam.getLevelId();
        dto.levelCode = levelCode;
        dto.title = exam.getTitle();
        dto.instructions = exam.getInstructions();
        dto.scheduledAt = exam.getScheduledAt();
        dto.status = exam.getStatus() != null ? exam.getStatus().name() : Exam.ExamStatus.DRAFT.name();
        dto.createdAt = exam.getCreatedAt();
        return dto;
    }
}
