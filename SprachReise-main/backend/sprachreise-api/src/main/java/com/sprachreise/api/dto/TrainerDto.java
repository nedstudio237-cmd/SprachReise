package com.sprachreise.api.dto;

import com.sprachreise.api.entity.TrainerProfile;

import java.math.BigDecimal;

public class TrainerDto {
    public Long id;
    public Long userId;
    public String firstName;
    public String lastName;
    public String photoUrl;
    public String bio;
    public String city;
    public Long assignedLevelId;
    public String assignedLevelCode;
    public Integer maxStudents;
    public Integer currentStudents;
    public Integer placesLeft;
    public BigDecimal ratingAvg;

    public static TrainerDto from(TrainerProfile tp, String levelCode) {
        TrainerDto dto = new TrainerDto();
        dto.id = tp.getId();
        dto.userId = tp.getUser().getId();
        dto.firstName = tp.getUser().getFirstName();
        dto.lastName = tp.getUser().getLastName();
        dto.photoUrl = tp.getUser().getPhotoUrl();
        dto.bio = tp.getUser().getBio();
        dto.city = tp.getUser().getCity();
        dto.assignedLevelId = tp.getAssignedLevelId();
        dto.assignedLevelCode = levelCode;
        dto.maxStudents = tp.getMaxStudents();
        dto.currentStudents = tp.getCurrentStudents();
        dto.placesLeft = Math.max(0, tp.getMaxStudents() - tp.getCurrentStudents());
        dto.ratingAvg = tp.getRatingAvg();
        return dto;
    }
}
