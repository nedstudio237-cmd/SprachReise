package com.sprachreise.api.dto;

import com.sprachreise.api.entity.ExamSubmission;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamSubmissionDto {
    public Long id;
    public Long examId;
    public Long learnerId;
    public String learnerName;
    public String learnerEmail;
    public String answerText;
    public LocalDateTime submittedAt;
    public BigDecimal grade;
    public String feedback;
    public LocalDateTime gradedAt;
    public Long gradedBy;

    public static ExamSubmissionDto from(ExamSubmission sub, String learnerName, String learnerEmail) {
        ExamSubmissionDto dto = new ExamSubmissionDto();
        dto.id = sub.getId();
        dto.examId = sub.getExamId();
        dto.learnerId = sub.getLearnerId();
        dto.learnerName = learnerName;
        dto.learnerEmail = learnerEmail;
        dto.answerText = sub.getAnswerText();
        dto.submittedAt = sub.getSubmittedAt();
        dto.grade = sub.getGrade();
        dto.feedback = sub.getFeedback();
        dto.gradedAt = sub.getGradedAt();
        dto.gradedBy = sub.getGradedBy();
        return dto;
    }
}
