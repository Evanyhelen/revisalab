package com.revisalab.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trail_id", nullable = false)
    private Trail trail;
    
    @Builder.Default
    private Integer completedModules = 0;
    
    @Builder.Default
    private Integer completedExercises = 0;
    
    @Builder.Default
    private Integer totalExercises = 0;
    
    @Builder.Default
    private Integer score = 0;
    
    @Builder.Default
    private Integer attempts = 0;
    
    @Builder.Default
    private Integer correctAnswers = 0;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Builder.Default
    private Boolean completed = false;
    
    @Builder.Default
    private Double progress = 0.0;
    
    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        lastActivity = LocalDateTime.now();
    }
    
    // Métodos utilitários mantidos
    public void updateProgress() {
        if (totalExercises > 0) {
            this.progress = (double) completedExercises / totalExercises;
        }
    }
    
    public void addCorrectAnswer() {
        this.correctAnswers++;
        this.attempts++;
        updateProgress();
    }
    
    public void addIncorrectAnswer() {
        this.attempts++;
        updateProgress();
    }
    
    // Método setter customizado para completed
    public void setCompleted(Boolean completed) {
        this.completed = completed;
        if (completed) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
