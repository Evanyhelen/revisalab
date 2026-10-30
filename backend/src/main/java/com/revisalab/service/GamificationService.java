package com.revisalab.service;

import com.revisalab.model.*;
import com.revisalab.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço de Gamificação.
 *
 * Responsabilidade única: desbloquear conquistas (achievements) automaticamente
 * conforme o usuário progride. As missões, ranking e estatísticas são expostas
 * diretamente pelo {@code GamificationController}.
 */
@Service
@Transactional
public class GamificationService {

    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final StudySessionRepository studySessionRepository;

    @Autowired
    public GamificationService(AchievementRepository achievementRepository,
                              UserRepository userRepository,
                              UserAchievementRepository userAchievementRepository,
                              UserAnswerRepository userAnswerRepository,
                              StudySessionRepository studySessionRepository) {
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.studySessionRepository = studySessionRepository;
    }

    /**
     * Verifica e desbloqueia conquistas automaticamente.
     * Chamado após cada ação importante do usuário (responder exercício,
     * concluir sessão etc.).
     *
     * @param user Usuário a verificar
     */
    public void checkAndUnlockAchievements(User user) {
        for (String code : List.of("STREAK_3", "STREAK_7", "LEVEL_5", "EXERCISES_50")) {
            Achievement achievement = achievementRepository.findByCode(code).orElse(null);
            if (achievement != null && calculateAchievementProgress(achievement, user) >= achievement.getRequiredValue()) {
                unlockAchievementIfNotAlready(user, achievement);
            }
        }
    }

    /**
     * Desbloqueia uma conquista caso ainda não esteja desbloqueada.
     */
    private void unlockAchievementIfNotAlready(User user, Achievement achievement) {
        boolean alreadyUnlocked = userAchievementRepository
            .findByUserIdAndAchievementId(user.getId(), achievement.getId())
            .isPresent();

        if (!alreadyUnlocked) {
            UserAchievement userAchievement = new UserAchievement();
            userAchievement.setUser(user);
            userAchievement.setAchievement(achievement);
            userAchievement.setUnlockedAt(LocalDateTime.now());
            userAchievementRepository.save(userAchievement);

            // Adicionar XP da conquista
            user.addXp(achievement.getXpReward());
            userRepository.save(user);
        }
    }

    /**
     * Calcula o progresso real de uma conquista a partir dos dados persistidos
     * (streak, nível e exercícios respondidos, incluindo sessões do ENEM/Construtor).
     */
    private int calculateAchievementProgress(Achievement achievement, User user) {
        switch (achievement.getCode()) {
            case "STREAK_3":
            case "STREAK_7":
                return Math.min(user.getStreak(), achievement.getRequiredValue());
            case "LEVEL_5":
                return Math.min(user.getLevel(), achievement.getRequiredValue());
            case "EXERCISES_50":
                long total = userAnswerRepository.countByUserId(user.getId())
                        + studySessionRepository.sumTotalQuestionsByUser(user.getId());
                return (int) Math.min(total, achievement.getRequiredValue());
            default:
                return 0;
        }
    }
}
