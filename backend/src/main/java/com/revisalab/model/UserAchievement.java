package com.revisalab.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAchievement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", nullable = false)
    private Achievement achievement;
    
    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;
    
    @Builder.Default
    private Integer progress = 0; // Progresso atual (para conquistas progressivas)
    
    @Builder.Default
    private Boolean completed = false;
    
    @PrePersist
    protected void onCreate() {
        unlockedAt = LocalDateTime.now();
    }
}
