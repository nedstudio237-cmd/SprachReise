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
    public String status;
    public Integer viewCount;
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
        dto.status = c.getStatus().name();
        dto.viewCount = c.getViewCount();
        if (c.getTrainer() != null) {
            dto.trainerName = c.getTrainer().getFirstName() + " " + c.getTrainer().getLastName();
        }
        return dto;
    }
}
