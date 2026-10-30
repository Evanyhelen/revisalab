package com.revisalab.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_missions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "mission_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer progress = 0; // Valor atual de progresso
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "reward_claimed")
    @Builder.Default
    private Boolean rewardClaimed = false;
    
    // Constructors mantidos para compatibilidade
    public UserMission(User user, Mission mission) {
        this.user = user;
        this.mission = mission;
        this.progress = 0;
        this.completed = false;
        this.rewardClaimed = false;
        this.startedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
    
    // Business Logic
    public void incrementProgress(Integer value) {
        this.progress += value;
        if (mission != null && this.progress >= mission.getRequiredValue() && !completed) {
            this.completed = true;
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public void claimReward() {
        if (completed && !rewardClaimed) {
            rewardClaimed = true;
        }
    }
    
    public boolean isReadyToClaim() {
        return completed && !rewardClaimed;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserMission that = (UserMission) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
