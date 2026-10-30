package com.revisalab.controller;

import com.revisalab.dto.request.PersonalMissionRequest;
import com.revisalab.model.Mission;
import com.revisalab.model.User;
import com.revisalab.model.UserAnswer;
import com.revisalab.model.UserMission;
import com.revisalab.repository.AchievementRepository;
import com.revisalab.repository.MissionRepository;
import com.revisalab.repository.UserAchievementRepository;
import com.revisalab.repository.StudySessionRepository;
import com.revisalab.repository.UserAnswerRepository;
import com.revisalab.repository.UserMissionRepository;
import com.revisalab.repository.UserRepository;
import com.revisalab.security.UserDetailsImpl;
import com.revisalab.service.GamificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gamification")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class GamificationController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AchievementRepository achievementRepository;
    
    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private UserMissionRepository userMissionRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private GamificationService gamificationService;

    @GetMapping("/achievements")
    public ResponseEntity<?> getUserAchievements(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        // Carrega o usuário fresco do banco: o principal do JWT pode estar defasado
        // (streak/level são atualizados a cada atividade e não refletem no token).
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Exercícios respondidos, incluindo as sessões do ENEM/Construtor
        // (que não geram UserAnswer), para a conquista EXERCISES_50 refletir tudo.
        long totalAnswers = userAnswerRepository.countByUserId(user.getId())
                + studySessionRepository.sumTotalQuestionsByUser(user.getId());

        List<Map<String, Object>> achievements = achievementRepository.findAll().stream()
                .map(achievement -> {
                    Map<String, Object> achievementData = new HashMap<>();
                    achievementData.put("id", achievement.getId());
                    achievementData.put("code", achievement.getCode());
                    achievementData.put("name", achievement.getName());
                    achievementData.put("description", achievement.getDescription());
                    achievementData.put("icon", achievement.getIcon());
                    achievementData.put("xpReward", achievement.getXpReward());
                    achievementData.put("type", achievement.getType());
                    achievementData.put("requiredValue", achievement.getRequiredValue());

                    int progress;
                    switch (achievement.getCode()) {
                        case "STREAK_3":
                            progress = Math.min(user.getStreak(), achievement.getRequiredValue());
                            break;
                        case "STREAK_7":
                            progress = Math.min(user.getStreak(), achievement.getRequiredValue());
                            break;
                        case "LEVEL_5":
                            progress = Math.min(user.getLevel(), achievement.getRequiredValue());
                            break;
                        case "EXERCISES_50":
                            progress = (int) Math.min(totalAnswers, achievement.getRequiredValue());
                            break;
                        default:
                            progress = 0;
                            break;
                    }

                    boolean unlocked = progress >= achievement.getRequiredValue();
                    achievementData.put("unlocked", unlocked);
                    achievementData.put("progress", progress);
                    achievementData.put("completed", unlocked);

                    // unlockedAt real, apenas quando a conquista foi efetivamente persistida.
                    userAchievementRepository.findByUserIdAndAchievementId(user.getId(), achievement.getId())
                            .ifPresent(ua -> achievementData.put("unlockedAt",
                                    ua.getUnlockedAt() != null ? ua.getUnlockedAt().toString() : LocalDateTime.now().toString()));

                    return achievementData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(achievements);
    }
    
    @GetMapping("/missions")
    public ResponseEntity<?> getUserMissions(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Missões ativas: globais (compartilhadas) + pessoais (criadas pelo usuário).
        List<Mission> activeMissions = new ArrayList<>();
        activeMissions.addAll(missionRepository.findGlobalActiveMissions());
        activeMissions.addAll(missionRepository.findPersonalActiveMissions(user));

        List<Map<String, Object>> missions = activeMissions.stream()
                .map(mission -> {
                    Map<String, Object> missionData = new HashMap<>();
                    missionData.put("id", mission.getId());
                    missionData.put("code", mission.getCode());
                    missionData.put("title", mission.getTitle());
                    missionData.put("description", mission.getDescription());
                    missionData.put("type", mission.getType());
                    missionData.put("xpReward", mission.getXpReward());
                    missionData.put("requiredValue", mission.getRequiredValue());
                    missionData.put("actionType", mission.getActionType());
                    missionData.put("personal", Boolean.TRUE.equals(mission.getIsPersonal()));

                    int progress = missionProgress(mission, user);

                    // "completed" considera o período da missão: missões diárias/semanais
                    // resetam automaticamente no dia/semana seguinte (senão ficariam
                    // "concluídas para sempre" e não poderiam ser reivindicadas de novo).
                    boolean completed = userMissionRepository.findByUserAndMission(user, mission)
                            .map(um -> isCompletedInCurrentPeriod(um, mission))
                            .orElse(false);
                    boolean claimable = progress >= mission.getRequiredValue() && !completed;

                    missionData.put("progress", progress);
                    missionData.put("completed", completed);
                    missionData.put("claimable", claimable);
                    missionData.put("available", mission.isAvailable());

                    return missionData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(missions);
    }
    
    @PostMapping("/missions/{missionId}/complete")
    public ResponseEntity<?> completeMission(@PathVariable Long missionId,
                                           Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        var missionOptional = missionRepository.findById(missionId);
        if (missionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var mission = missionOptional.get();

        // Missões pessoais só podem ser concluídas pelo seu dono.
        if (Boolean.TRUE.equals(mission.getIsPersonal())
                && (mission.getUser() == null || !mission.getUser().getId().equals(user.getId()))) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        
        // Verificar se a missão já está disponível e não foi completada
        if (!mission.isAvailable()) {
            response.put("success", false);
            response.put("message", "Missão não disponível");
            return ResponseEntity.badRequest().body(response);
        }

        // Valida o progresso real: só concede XP quando o objetivo foi atingido.
        int progress = missionProgress(mission, user);
        if (progress < mission.getRequiredValue()) {
            response.put("success", false);
            response.put("message", "Você ainda não atingiu o objetivo desta missão.");
            response.put("progress", progress);
            response.put("requiredValue", mission.getRequiredValue());
            return ResponseEntity.badRequest().body(response);
        }

        // Impede recompensa duplicada dentro do mesmo período (XP infinito):
        // missão diária/semanal pode ser reivindicada novamente só no período seguinte.
        var existing = userMissionRepository.findByUserAndMission(user, mission);
        if (existing.map(um -> isCompletedInCurrentPeriod(um, mission)).orElse(false)) {
            response.put("success", false);
            response.put("message", "Missão já concluída");
            response.put("xpEarned", 0);
            response.put("userXp", user.getXp());
            response.put("userLevel", user.getLevel());
            return ResponseEntity.badRequest().body(response);
        }

        // Registra a conclusão da missão (reaproveitando a linha no caso de missão
        // periódica) e concede XP. O XP é entregue de imediato, então marcamos a
        // recompensa como já reclamada para manter a contabilidade consistente.
        UserMission userMission = existing.orElseGet(() -> new UserMission(user, mission));
        userMission.setProgress(mission.getRequiredValue());
        userMission.setCompleted(true);
        userMission.setCompletedAt(LocalDateTime.now());
        userMission.setRewardClaimed(true);
        userMissionRepository.save(userMission);

        user.addXp(mission.getXpReward());
        // Conceder XP de missão também pode cruzar o limite de um nível; verifica
        // conquistas (ex.: Mestre em Formação) sem exigir um exercício posterior.
        gamificationService.checkAndUnlockAchievements(user);
        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Missão completada com sucesso!");
        response.put("xpEarned", mission.getXpReward());
        response.put("userXp", user.getXp());
        response.put("userLevel", user.getLevel());

        return ResponseEntity.ok(response);
    }

    /**
     * Cria uma missão pessoal com meta e recompensa validadas no backend.
     * O XP é concedido apenas na conclusão (via /missions/{id}/complete),
     * nunca no ato da criação.
     */
    @PostMapping("/personal-missions")
    public ResponseEntity<?> createPersonalMission(@Valid @RequestBody PersonalMissionRequest request,
                                                   Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Limite de missões pessoais ativas para evitar abuso.
        long activeCount = missionRepository.findPersonalActiveMissions(user).size();
        if (activeCount >= 10) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Limite de 10 missões pessoais ativas atingido."
            ));
        }

        Mission mission = new Mission();
        mission.setCode("PERSONAL_" + UUID.randomUUID().toString().replace("-", ""));
        mission.setTitle(request.getTitle().trim());
        mission.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        mission.setType(request.getType());
        mission.setActionType(request.getActionType());
        mission.setRequiredValue(request.getRequiredValue());
        mission.setXpReward(request.getXpReward());
        mission.setActive(true);
        mission.setIsPersonal(true);
        mission.setUser(user);
        missionRepository.save(mission);

        Map<String, Object> response = new HashMap<>();
        response.put("id", mission.getId());
        response.put("title", mission.getTitle());
        response.put("message", "Missão pessoal criada com sucesso!");
        return ResponseEntity.ok(response);
    }

    /**
     * Exclui uma missão pessoal do usuário (apenas as ainda não concluídas).
     */
    @DeleteMapping("/personal-missions/{missionId}")
    public ResponseEntity<?> deletePersonalMission(@PathVariable Long missionId,
                                                   Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var missionOptional = missionRepository.findByIdAndUserAndIsPersonalTrue(missionId, user);
        if (missionOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        missionRepository.delete(missionOptional.get());
        return ResponseEntity.ok(Map.of("message", "Missão pessoal removida."));
    }

    @GetMapping("/ranking")
    public ResponseEntity<?> getRanking(@RequestParam(defaultValue = "xp") String sortBy,
                                       @RequestParam(defaultValue = "10") int limit) {
        // Exclui contas de teste automatizado para não poluir a classificação.
        List<User> users = userRepository.findAllForRanking();

        // Ordenar conforme o parâmetro
        List<Map<String, Object>> ranking = users.stream()
                .sorted((u1, u2) -> {
                    switch (sortBy.toLowerCase()) {
                        case "level":
                            return u2.getLevel().compareTo(u1.getLevel());
                        case "streak":
                            return u2.getStreak().compareTo(u1.getStreak());
                        case "xp":
                        default:
                            return u2.getXp().compareTo(u1.getXp());
                    }
                })
                .limit(limit)
                .map(user -> {
                    Map<String, Object> rankData = new HashMap<>();
                    rankData.put("position", 0); // Será calculado
                    rankData.put("id", user.getId());
                    rankData.put("name", user.getName());
                    rankData.put("xp", user.getXp());
                    rankData.put("level", user.getLevel());
                    rankData.put("streak", user.getStreak());
                    rankData.put("createdAt", user.getCreatedAt());
                    return rankData;
                })
                .collect(Collectors.toList());

        // Adicionar posições
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("position", i + 1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sortBy", sortBy);
        response.put("ranking", ranking);
        response.put("totalUsers", users.size());

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user-stats")
    public ResponseEntity<?> getUserGamificationStats(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Posição real no ranking (por XP): conta quantos usuários visíveis têm mais XP.
        long usersAhead = userRepository.findAllForRanking().stream()
                .filter(u -> u.getXp() > user.getXp())
                .count();
        int rank = (int) usersAhead + 1;

        Map<String, Object> stats = new HashMap<>();
        stats.put("xp", user.getXp());
        stats.put("level", user.getLevel());
        stats.put("streak", user.getStreak());
        stats.put("totalAchievements", (int) achievementRepository.count());
        stats.put("completedMissions", userMissionRepository.countCompletedMissionsByUser(user).intValue());
        stats.put("rank", rank);

        // Próximo nível
        int xpForNextLevel = (user.getLevel() * 1000) - user.getXp();
        stats.put("xpForNextLevel", xpForNextLevel);
        stats.put("xpProgress", (double) user.getXp() % 1000 / 1000 * 100);

        // Streak: sem rastreamento de máximo histórico, o streak atual é o piso real.
        stats.put("maxStreak", user.getStreak());
        stats.put("streakHistory", buildStreakHistory(user));

        return ResponseEntity.ok(stats);
    }

    /**
     * Calcula o progresso real de uma missão a partir dos dados persistidos
     * (respostas, sessões e tempo de estudo) — sem valores simulados.
     */
    private int missionProgress(Mission mission, User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = periodStart(mission);
        String actionType = mission.getActionType() == null ? "" : mission.getActionType();

        switch (actionType) {
            case "complete_exercises": {
                int answers = userAnswerRepository
                        .findByUserAndSubmittedAtBetween(user, start, now).size();
                int sessionQuestions = (int) studySessionRepository
                        .sumTotalQuestionsByUserSince(user.getId(), start);
                return Math.min(answers + sessionQuestions, mission.getRequiredValue());
            }
            case "study_minutes": {
                long seconds = userAnswerRepository
                        .findByUserAndSubmittedAtBetween(user, start, now).stream()
                        .filter(a -> a.getTimeSpentSeconds() != null)
                        .mapToLong(UserAnswer::getTimeSpentSeconds).sum()
                        + studySessionRepository.sumDurationSecondsByUserSince(user.getId(), start);
                return Math.min((int) (seconds / 60), mission.getRequiredValue());
            }
            case "streak":
                // "Estude hoje" só conta se houve atividade registrada no dia atual;
                // streak > 0 sozinho ficaria defasado (mantém o último valor histórico).
                return (user.getLastActivity() != null
                        && user.getLastActivity().toLocalDate().equals(LocalDate.now())) ? 1 : 0;
            case "complete_trail":
                // A missão semanal "Domine a Matemática" conta uma lição de Matemática
                // concluída nos últimos 7 dias.
                return (int) Math.min(studySessionRepository
                        .countByUserAndCategorySince(user.getId(), "Matemática", now.minusDays(7)),
                        mission.getRequiredValue());
            default:
                return 0;
        }
    }

    /**
     * Início da janela de contagem conforme a periodicidade da missão:
     * missões semanais consideram os últimos 7 dias; as demais, o dia atual.
     */
    private LocalDateTime periodStart(Mission mission) {
        return "weekly".equals(mission.getType())
                ? LocalDateTime.now().minusDays(7)
                : LocalDate.now().atStartOfDay();
    }

    /**
     * Indica se a missão concluída ainda vale no período atual. Missões "daily"
     * valem só no dia da conclusão; "weekly", nos últimos 7 dias; demais tipos
     * permanecem concluídas indefinidamente.
     */
    private boolean isCompletedInCurrentPeriod(UserMission um, Mission mission) {
        if (um == null || !Boolean.TRUE.equals(um.getCompleted()) || um.getCompletedAt() == null) {
            return false;
        }
        LocalDate completedDay = um.getCompletedAt().toLocalDate();
        LocalDate today = LocalDate.now();
        String type = mission.getType() == null ? "" : mission.getType();
        return switch (type) {
            case "daily" -> completedDay.equals(today);
            case "weekly" -> !completedDay.isBefore(today.minusDays(6));
            default -> true;
        };
    }

    /**
     * Reconstrói o histórico dos últimos 7 dias a partir do streak e da última
     * atividade registrados — sem datas simuladas.
     */
    private List<Map<String, Object>> buildStreakHistory(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastStudied = user.getLastActivity() != null ? user.getLastActivity().toLocalDate() : null;
        int streak = user.getStreak();

        List<Map<String, Object>> history = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            boolean studied = lastStudied != null && streak > 0
                    && !day.isAfter(lastStudied)
                    && !day.isBefore(lastStudied.minusDays(streak - 1L));
            Map<String, Object> entry = new HashMap<>();
            entry.put("date", day.toString());
            entry.put("studied", studied);
            history.add(entry);
        }
        return history;
    }
}