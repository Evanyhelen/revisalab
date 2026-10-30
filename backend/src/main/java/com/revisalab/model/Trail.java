package com.revisalab.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "trails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"modules", "learningProgresses"})
public class Trail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private String category; // Ex: Matemática, Português, Física, etc.
    
    @Builder.Default
    private Integer totalModules = 0;
    
    @Builder.Default
    private Integer totalExercises = 0;
    
    @Builder.Default
    private Integer estimatedHours = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer difficulty = 1; // 1-5
    
    private String icon;
    
    private String color;
    
    @OneToMany(mappedBy = "trail", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Module> modules = new HashSet<>();
    
    @OneToMany(mappedBy = "trail", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<LearningProgress> learningProgresses = new HashSet<>();
    
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
    
    // Método para atualizar contagens
    public void updateCounts() {
        this.totalModules = modules.size();
        this.totalExercises = modules.stream()
            .mapToInt(Module::getTotalExercises)
            .sum();
    }
}
