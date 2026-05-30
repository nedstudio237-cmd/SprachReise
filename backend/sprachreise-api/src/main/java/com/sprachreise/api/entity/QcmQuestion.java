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

    @Column(name = "order_index")
    private Integer orderIndex;

    @OneToMany(mappedBy = "question", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<QcmChoice> choices;

    public Long getId() { return id; }
    public Qcm getQcm() { return qcm; }
    public String getQuestionText() { return questionText; }
    public Integer getOrderIndex() { return orderIndex; }
    public List<QcmChoice> getChoices() { return choices; }
}
