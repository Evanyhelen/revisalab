package com.revisalab.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;

@Entity
@Table(name = "answer_options")
@Data
public class AnswerOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
    
    @Column(nullable = false, length = 1000)
    private String text;
    
    @Column(nullable = false)
    private Integer displayOrder;
    
    // Constructors
    public AnswerOption() {}
    
    public AnswerOption(Exercise exercise, String text, Integer displayOrder) {
        this.exercise = exercise;
        this.text = text;
        this.displayOrder = displayOrder;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnswerOption that = (AnswerOption) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
