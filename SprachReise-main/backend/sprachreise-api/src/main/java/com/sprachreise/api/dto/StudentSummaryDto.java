package com.sprachreise.api.dto;

import java.math.BigDecimal;

public class StudentSummaryDto {
    public Long id;
    public String firstName;
    public String lastName;
    public String photoUrl;
    public String levelCode;
    public BigDecimal completionPercentage;
    public Boolean certified;
    public String lastActiveAt;
    public String status;
}
