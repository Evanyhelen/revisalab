package com.revisalab.repository;

import com.revisalab.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para Conquista do Usuário
 * Acesso aos dados de conquistas desbloqueadas pelos usuários
 */
@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    /**
     * Encontra conquista desbloqueada para um usuário específico
     * @param userId ID do usuário
     * @param achievementId ID da conquista
     * @return Conquista desbloqueada se existir
     */
    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);

    /**
     * Conta quantas conquistas um usuário desbloqueou
     * @param userId ID do usuário
     * @return Quantidade de conquistas desbloqueadas
     */
    long countByUserId(Long userId);

    /**
     * Lista as conquistas desbloqueadas pelo usuário, mais recentes primeiro,
     * com o relacionamento {@code achievement} já carregado (evita LazyInitialization).
     */
    @Query("SELECT ua FROM UserAchievement ua JOIN FETCH ua.achievement WHERE ua.user.id = :userId ORDER BY ua.unlockedAt DESC")
    List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(@Param("userId") Long userId);
}
