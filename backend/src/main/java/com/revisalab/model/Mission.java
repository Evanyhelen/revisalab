package com.revisalab.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "missions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private String type; // "daily", "weekly", "monthly", "special"
    
    @Builder.Default
    private Integer xpReward = 0;
    
    @Builder.Default
    private Integer requiredValue = 0;
    
    @Column(nullable = false)
    private String actionType; // "complete_exercises", "study_minutes", "complete_trail", "streak", "correct_streak", "review_errors"

    @Builder.Default
    private Boolean active = true;

    /**
     * Missões pessoais pertencem a um único usuário (criadas pelo estudante).
     * Missões globais têm {@code user} nulo e {@code isPersonal = false}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_personal")
    @Builder.Default
    private Boolean isPersonal = false;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
    
    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<UserMission> userMissions = new HashSet<>();
    
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
    
    // Método para verificar se a missão está disponível
    public boolean isAvailable() {
        if (!active) return false;
        
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;
        
        return true;
    }
}
