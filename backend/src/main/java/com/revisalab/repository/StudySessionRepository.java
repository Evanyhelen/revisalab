package com.revisalab.repository;

import com.revisalab.model.StudySession;
import com.revisalab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByUser(User user);

    /**
     * Soma os acertos de sessões concluídas pelo usuário numa categoria, para
     * alimentar o progresso das trilhas também a partir da prática do ENEM.
     */
    @Query("SELECT COALESCE(SUM(s.correctAnswers), 0) FROM StudySession s " +
            "WHERE s.user.id = :userId AND s.category = :category")
    long sumCorrectAnswersByUserAndCategory(@Param("userId") Long userId,
                                            @Param("category") String category);

    /**
     * Total de questões respondidas em sessões (ENEM/Construtor), para que
     * conquistas/missões que contam "exercícios" não ignorem a prática externa.
     */
    @Query("SELECT COALESCE(SUM(s.totalQuestions), 0) FROM StudySession s WHERE s.user.id = :userId")
    long sumTotalQuestionsByUser(@Param("userId") Long userId);

    /**
     * Número de sessões concluídas pelo usuário numa categoria desde uma data
     * (ex.: "Matemática" nos últimos 7 dias para a missão semanal).
     */
    @Query("SELECT COUNT(s) FROM StudySession s WHERE s.user.id = :userId " +
            "AND s.category LIKE %:category% AND s.completedAt >= :since")
    long countByUserAndCategorySince(@Param("userId") Long userId,
                                     @Param("category") String category,
                                     @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(s.totalQuestions), 0) FROM StudySession s " +
            "WHERE s.user.id = :userId AND s.completedAt >= :since")
    long sumTotalQuestionsByUserSince(@Param("userId") Long userId,
                                      @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(s.durationSeconds), 0) FROM StudySession s WHERE s.user.id = :userId")
    long sumDurationSecondsByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(s.durationSeconds), 0) FROM StudySession s " +
            "WHERE s.user.id = :userId AND s.completedAt >= :since")
    long sumDurationSecondsByUserSince(@Param("userId") Long userId,
                                       @Param("since") LocalDateTime since);

    List<StudySession> findByUserAndCompletedAtAfter(User user, LocalDateTime since);
}
