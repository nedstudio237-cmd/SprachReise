package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "qcm_questions")
public class QcmQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qcm_id")
    private Qcm qcm;

    @Column(name = "question_text", columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType = QuestionType.SINGLE_CHOICE;

    @Column(name = "order_index")
    private Integer orderIndex;

    @OneToMany(mappedBy = "question", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QcmChoice> choices;

    public enum QuestionType { SINGLE_CHOICE, MULTI_CHOICE }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Qcm getQcm() { return qcm; }
    public void setQcm(Qcm qcm) { this.qcm = qcm; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public List<QcmChoice> getChoices() { return choices; }
    public void setChoices(List<QcmChoice> choices) { this.choices = choices; }
}
