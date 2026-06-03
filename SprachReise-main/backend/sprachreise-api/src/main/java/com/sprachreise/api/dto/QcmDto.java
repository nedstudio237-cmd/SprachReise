package com.sprachreise.api.dto;

import com.sprachreise.api.entity.Qcm;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class QcmDto {
    public Long id;
    public Long levelId;
    public String title;
    public String theme;
    public Long createdBy;
    public LocalDateTime scheduledAt;
    public String status;
    public List<QuestionDto> questions;

    public static class QuestionDto {
        public Long id;
        public String questionText;
        public String questionType;
        public Integer orderIndex;
        public List<ChoiceDto> choices;
    }

    public static class ChoiceDto {
        public Long id;
        public String choiceText;
        public Boolean isCorrect;
        public String explanation;
    }

    public static QcmDto from(Qcm qcm) {
        QcmDto dto = new QcmDto();
        dto.id = qcm.getId();
        dto.levelId = qcm.getLevelId();
        dto.title = qcm.getTitle();
        dto.theme = qcm.getTheme();
        dto.createdBy = qcm.getCreatedBy();
        dto.scheduledAt = qcm.getScheduledAt();
        dto.status = qcm.getStatus() != null ? qcm.getStatus().name() : Qcm.QcmStatus.DRAFT.name();
        if (qcm.getQuestions() != null) {
            dto.questions = qcm.getQuestions().stream().map(q -> {
                QuestionDto qd = new QuestionDto();
                qd.id = q.getId();
                qd.questionText = q.getQuestionText();
                qd.questionType = q.getQuestionType() != null ? q.getQuestionType().name() : "SINGLE_CHOICE";
                qd.orderIndex = q.getOrderIndex();
                if (q.getChoices() != null) {
                    qd.choices = q.getChoices().stream().map(c -> {
                        ChoiceDto cd = new ChoiceDto();
                        cd.id = c.getId();
                        cd.choiceText = c.getChoiceText();
                        cd.isCorrect = c.getIsCorrect();
                        cd.explanation = c.getExplanation();
                        return cd;
                    }).collect(Collectors.toList());
                }
                return qd;
            }).collect(Collectors.toList());
        }
        return dto;
    }
}
