package com.revisalab.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidade que representa um usuário da plataforma RevisaLab
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"password", "roles", "achievements", "learningProgresses"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 50, message = "Nome deve ter no máximo 50 caracteres")
    @Column(nullable = false, length = 50)
    private String name;
    
    @NotBlank(message = "Email é obrigatório")
    @Size(max = 100, message = "Email deve ter no máximo 100 caracteres")
    @Email(message = "Email deve ser válido")
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @NotBlank(message = "Senha é obrigatória")
    @Size(max = 120, message = "Senha deve ter no máximo 120 caracteres")
    @Column(nullable = false, length = 120)
    private String password;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer xp = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer level = 1;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer streak = 0;

    @Column(length = 100)
    private String curso;

    @Column(length = 30)
    private String semestre;

    @Column(length = 100)
    private String instituicao;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "last_chat_xp_date")
    private LocalDate lastChatXpDate;

    @Column(name = "daily_chat_xp")
    private Integer dailyChatXp;

    @Column(name = "last_builder_xp_date")
    private LocalDate lastBuilderXpDate;

    @Column(name = "daily_builder_xp")
    private Integer dailyBuilderXp;

    @Column(name = "last_session_xp_date")
    private LocalDate lastSessionXpDate;

    @Column(name = "daily_session_xp")
    private Integer dailySessionXp;

    /**
     * Indica se o usuário é um dado de demonstração/exemplo (seed da plataforma).
     * Dados de demonstração são preservados e exibidos normalmente.
     */
    @Column(name = "is_demo")
    @Builder.Default
    private Boolean isDemo = false;

    /**
     * Indica se o usuário é uma conta criada por testes automatizados.
     * Contas de teste são ocultadas do ranking/painéis para não poluir a
     * classificação dos usuários reais e dos dados de demonstração.
     */
    @Column(name = "is_test")
    @Builder.Default
    private Boolean isTest = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserAchievement> achievements = new HashSet<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<LearningProgress> learningProgresses = new HashSet<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (xp == null) xp = 0;
        if (level == null) level = 1;
        if (streak == null) streak = 0;
        if (isDemo == null) isDemo = false;
        if (isTest == null) isTest = false;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Adiciona XP ao usuário e calcula o nível
     */
    public void addXp(int points) {
        this.xp += points;
        calculateLevel();
    }
    
    /**
     * Calcula o nível baseado no XP.
     * Fórmula: Nível = XP / 1000 + 1.
     */
    private void calculateLevel() {
        this.level = this.xp / 1000 + 1;
    }
    
    /**
     * Registra a atividade do usuário hoje, mantendo o streak de dias consecutivos.
     *
     * Regras:
     * - Primeira atividade (ou após uma lacuna de mais de um dia) → streak = 1.
     * - Atividade no dia seguinte ao último registro → streak incrementa.
     * - Mais de uma atividade no mesmo dia → streak inalterado.
     */
    public void registerDailyActivity() {
        LocalDate today = LocalDate.now();
        LocalDate last = lastActivity != null ? lastActivity.toLocalDate() : null;

        if (last == null || last.isBefore(today.minusDays(1))) {
            this.streak = 1;
        } else if (last.equals(today.minusDays(1))) {
            this.streak++;
        }
        // last.equals(today): já contabilizado hoje, não altera.

        this.lastActivity = LocalDateTime.now();
    }
}