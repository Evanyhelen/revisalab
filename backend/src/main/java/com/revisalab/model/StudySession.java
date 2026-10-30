package com.revisalab.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Sessão de estudo concluída (quiz ENEM ou Construtor de Base).
 *
 * Diferente de {@link UserAnswer} — que registra a resposta a um {@link Exercise}
 * individual —, esta entidade registra o agregado de uma sessão cujas questões
 * vêm de fontes externas (API pública do ENEM) e, portanto, não possuem um
 * {@code Exercise} persistido. É o que alimenta o painel de desempenho com a
 * atividade de quem pratica pelo quiz do ENEM ou pelo Construtor de Base.
 */
@Entity
@Table(name = "study_sessions", indexes = {
        @Index(name = "idx_ss_user", columnList = "user_id"),
        @Index(name = "idx_ss_completed", columnList = "completed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Rótulo exibível da sessão (ex.: "ENEM · Matemática", "Construtor de Base"). */
    @Column(nullable = false, length = 120)
    private String category;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;

    /** Duração da sessão em segundos (rastreada pelo cliente ao finalizar). */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudySession that = (StudySession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
