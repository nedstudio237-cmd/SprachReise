package com.sprachreise.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class StudentDetailDto {
    public Long id;
    public String firstName;
    public String lastName;
    public String email;
    public String photoUrl;
    public String levelCode;
    public BigDecimal completionPercentage;
    public Boolean certified;
    public String lastActiveAt;
    public String status;

    public Integer coursesCompleted;
    public Integer qcmAttemptsCount;
    public BigDecimal qcmAvgScore;
    public Integer sessionsAttended;
    public Integer totalMinutes;

    public List<MessageDto> recentMessages;
}
