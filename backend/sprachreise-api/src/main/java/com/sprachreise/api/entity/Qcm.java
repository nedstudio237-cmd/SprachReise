package com.sprachreise.api.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "qcms")
public class Qcm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_id")
    private Long levelId;

    private String title;
    private String theme;

    @OneToMany(mappedBy = "qcm", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("orderIndex ASC")
    private List<QcmQuestion> questions;

    public Long getId() { return id; }
    public Long getLevelId() { return levelId; }
    public String getTitle() { return title; }
    public String getTheme() { return theme; }
    public List<QcmQuestion> getQuestions() { return questions; }
}
