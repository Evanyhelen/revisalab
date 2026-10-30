package com.revisalab.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "exercises")
@Data
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"module", "answerOptions", "userAnswers"})
public class Exercise {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 2000)
    private String question;
    
    @Column(nullable = false)
    private String type; // multiple-choice, true-false, open-ended
    
    private String explanation;
    
    @Column(nullable = false)
    private String correctAnswer; // Resposta correta
    
    private Integer difficulty = 1; // 1-5
    
    private Integer xpReward = 10;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;
    
    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL)
    private Set<AnswerOption> answerOptions = new HashSet<>();
    
    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL)
    private Set<UserAnswer> userAnswers = new HashSet<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}