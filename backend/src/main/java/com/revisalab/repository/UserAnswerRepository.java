package com.revisalab.repository;

import com.revisalab.model.UserAnswer;
import com.revisalab.model.User;
import com.revisalab.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    List<UserAnswer> findByUser(User user);
    List<UserAnswer> findByExercise(Exercise exercise);
    List<UserAnswer> findByUserAndExercise(User user, Exercise exercise);
    
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.user = :user AND ua.exercise = :exercise ORDER BY ua.submittedAt DESC LIMIT 1")
    Optional<UserAnswer> findLastAnswerByUserAndExercise(@Param("user") User user, @Param("exercise") Exercise exercise);
    
    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.user = :user AND ua.isCorrect = true")
    Long countCorrectAnswersByUser(@Param("user") User user);

    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.user = :user AND ua.exercise = :exercise AND ua.isCorrect = true")
    Long countCorrectAnswersByUserAndExercise(@Param("user") User user, @Param("exercise") Exercise exercise);

    List<UserAnswer> findByUserAndSubmittedAtBetween(User user, LocalDateTime startDate, LocalDateTime endDate);

    List<UserAnswer> findByIsCorrectOrderBySubmittedAtDesc(Boolean isCorrect);

    /**
     * Conta exercícios distintos respondidos corretamente pelo usuário em uma trilha.
     */
    @Query("SELECT COUNT(DISTINCT ua.exercise.id) FROM UserAnswer ua " +
            "WHERE ua.user.id = :userId AND ua.exercise.module.trail.id = :trailId AND ua.isCorrect = true")
    Long countDistinctCorrectExercisesForUserAndTrail(@Param("userId") Long userId, @Param("trailId") Long trailId);

    /**
     * Conta exercícios distintos respondidos corretamente pelo usuário em um módulo.
     */
    @Query("SELECT COUNT(DISTINCT ua.exercise.id) FROM UserAnswer ua " +
            "WHERE ua.user.id = :userId AND ua.exercise.module.id = :moduleId AND ua.isCorrect = true")
    Long countDistinctCorrectExercisesForUserAndModule(@Param("userId") Long userId, @Param("moduleId") Long moduleId);

    /**
     * Conta o total de respostas do usuário.
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * Soma o tempo de estudo registrado (em segundos) em todas as respostas do usuário.
     */
    @Query("SELECT COALESCE(SUM(ua.timeSpentSeconds), 0) FROM UserAnswer ua WHERE ua.user = :user")
    Long sumTimeSpentSecondsByUser(@Param("user") User user);
}
