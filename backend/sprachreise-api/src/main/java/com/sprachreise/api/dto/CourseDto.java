package com.sprachreise.api.dto;

import com.sprachreise.api.entity.Course;

public class CourseDto {
    public Long id;
    public Long levelId;
    public String levelCode;
    public String title;
    public String description;
    public String theme;
    public Integer videoDurationSec;
    public String videoPath;
    public String pdfPath;
    public Long pdfSizeBytes;
    public String status;
    public Integer viewCount;
    public String publishAt;
    public String createdAt;
    public Long trainerId;
    public String trainerName;

    public static CourseDto from(Course c, String levelCode) {
        CourseDto dto = new CourseDto();
        dto.id = c.getId();
        dto.levelId = c.getLevelId();
        dto.levelCode = levelCode;
        dto.title = c.getTitle();
        dto.description = c.getDescription();
        dto.theme = c.getTheme();
        dto.videoDurationSec = c.getVideoDurationSec();
        dto.videoPath = c.getVideoPath();
        dto.pdfPath = c.getPdfPath();
        dto.pdfSizeBytes = c.getPdfSizeBytes();
        dto.status = c.getStatus().name();
        dto.viewCount = c.getViewCount();
        dto.publishAt = c.getPublishAt() != null ? c.getPublishAt().toString() : null;
        dto.createdAt = c.getCreatedAt() != null ? c.getCreatedAt().toString() : null;
        if (c.getTrainer() != null) {
            dto.trainerId = c.getTrainer().getId();
            dto.trainerName = c.getTrainer().getFirstName() + " " + c.getTrainer().getLastName();
        }
        return dto;
    }
}
