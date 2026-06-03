package com.sprachreise.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "qcm_choices")
public class QcmChoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QcmQuestion question;

    @Column(name = "choice_text", columnDefinition = "TEXT")
    private String choiceText;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public QcmQuestion getQuestion() { return question; }
    public void setQuestion(QcmQuestion question) { this.question = question; }

    public String getChoiceText() { return choiceText; }
    public void setChoiceText(String choiceText) { this.choiceText = choiceText; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
