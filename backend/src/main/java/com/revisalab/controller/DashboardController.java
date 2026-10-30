package com.revisalab.controller;

import com.revisalab.model.Achievement;
import com.revisalab.model.Trail;
import com.revisalab.model.User;
import com.revisalab.model.UserAchievement;
import com.revisalab.model.UserAnswer;
import com.revisalab.model.StudySession;
import com.revisalab.repository.AchievementRepository;
import com.revisalab.repository.ExerciseRepository;
import com.revisalab.repository.StudySessionRepository;
import com.revisalab.repository.TrailRepository;
import com.revisalab.repository.UserAchievementRepository;
import com.revisalab.repository.UserAnswerRepository;
import com.revisalab.repository.UserRepository;
import com.revisalab.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dashboard")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAnswerRepository userAnswerRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private TrailRepository trailRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("xp", user.getXp());
        stats.put("level", user.getLevel());
        stats.put("streak", user.getStreak());
        stats.put("name", user.getName());
        stats.put("email", user.getEmail());

        // Estatísticas reais: respostas individuais + questões de sessões
        // (ENEM/Construtor), para que o total e a acuracidade do dashboard batam
        // com o painel de desempenho e com a conquista "Resolvedor de Problemas".
        List<StudySession> sessions = studySessionRepository.findByUser(user);
        long sessionQuestions = sessions.stream().mapToLong(StudySession::getTotalQuestions).sum();
        long sessionCorrect = sessions.stream().mapToLong(StudySession::getCorrectAnswers).sum();
        long totalAnswers = userAnswerRepository.countByUserId(user.getId()) + sessionQuestions;
        long correctAnswers = userAnswerRepository.countCorrectAnswersByUser(user) + sessionCorrect;
        double accuracyRate = totalAnswers > 0
                ? (correctAnswers * 100.0 / totalAnswers)
                : 0.0;

        stats.put("totalExercises", totalAnswers);
        stats.put("correctAnswers", correctAnswers);
        stats.put("completedTrails", countCompletedTrails(user));

        // Tempo de estudo real: soma do tempo por resposta + duração das sessões.
        // totalStudyTime é exibido em horas ("h") no frontend; totalStudyTimeSeconds
        // preserva o valor bruto para consultas/precisão.
        long totalSeconds = userAnswerRepository.sumTimeSpentSecondsByUser(user)
                + studySessionRepository.sumDurationSecondsByUser(user.getId());
        stats.put("totalStudyTime", Math.round((totalSeconds / 3600.0) * 10.0) / 10.0);
        stats.put("totalStudyTimeSeconds", totalSeconds);

        stats.put("accuracyRate", Math.round(accuracyRate * 10.0) / 10.0);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard() {
        // Exclui contas de teste automatizado para não poluir o preview do ranking.
        List<User> topUsers = userRepository.findAllForRanking()
                .stream()
                .sorted((u1, u2) -> u2.getXp().compareTo(u1.getXp()))
                .limit(10)
                .collect(Collectors.toList());

        List<Map<String, Object>> leaderboard = topUsers.stream()
                .map(user -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", user.getId());
                    entry.put("name", user.getName());
                    entry.put("xp", user.getXp());
                    entry.put("level", user.getLevel());
                    entry.put("streak", user.getStreak());
                    return entry;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getUserProgress(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> progress = new HashMap<>();
        progress.put("trailProgress", buildTrailProgress(user));
        progress.put("weeklyActivity", buildWeeklyActivity(user));
        progress.put("recentAchievements", buildRecentAchievements(user));

        return ResponseEntity.ok(progress);
    }

    private int countCompletedTrails(User user) {
        int completed = 0;
        for (Trail trail : trailRepository.findAll()) {
            int total = exerciseRepository.findByModuleTrailId(trail.getId()).size();
            if (total > 0) {
                long correct = userAnswerRepository
                        .countDistinctCorrectExercisesForUserAndTrail(user.getId(), trail.getId());
                if (correct >= total) completed++;
            }
        }
        return completed;
    }

    private Map<String, Double> buildTrailProgress(User user) {
        Map<String, Double> trailProgress = new HashMap<>();
        for (Trail trail : trailRepository.findAll()) {
            int total = exerciseRepository.findByModuleTrailId(trail.getId()).size();
            if (total > 0) {
                long correct = userAnswerRepository
                        .countDistinctCorrectExercisesForUserAndTrail(user.getId(), trail.getId());
                trailProgress.put(trail.getCategory(), Math.min(1.0, (double) correct / total));
            }
        }
        return trailProgress;
    }

    private List<Map<String, Object>> buildWeeklyActivity(User user) {
        List<Map<String, Object>> weekly = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        // Sessões (ENEM/Construtor) concluídas na janela, para somar seu tempo por dia.
        Map<LocalDate, Long> sessionSecondsByDay = new HashMap<>();
        for (StudySession session : studySessionRepository.findByUserAndCompletedAtAfter(
                user, weekStart.atStartOfDay())) {
            LocalDate day = session.getCompletedAt() != null
                    ? session.getCompletedAt().toLocalDate() : today;
            long dur = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0L;
            sessionSecondsByDay.merge(day, dur, Long::sum);
        }

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<UserAnswer> dayAnswers = userAnswerRepository.findByUserAndSubmittedAtBetween(
                    user, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
            long timeSeconds = dayAnswers.stream()
                    .filter(a -> a.getTimeSpentSeconds() != null)
                    .mapToLong(UserAnswer::getTimeSpentSeconds)
                    .sum()
                    + sessionSecondsByDay.getOrDefault(day, 0L);

            // Questões do dia vindas de sessões (ENEM/Construtor) concluídas exatamente neste dia.
            long daySessionQuestions = studySessionRepository.findByUserAndCompletedAtAfter(
                    user, day.atStartOfDay()).stream()
                    .filter(s -> s.getCompletedAt() != null
                            && s.getCompletedAt().toLocalDate().equals(day))
                    .mapToLong(StudySession::getTotalQuestions)
                    .sum();

            Map<String, Object> entry = new HashMap<>();
            entry.put("day", dayLabel(day.getDayOfWeek()));
            entry.put("exercises", dayAnswers.size() + (int) daySessionQuestions);
            entry.put("time", (int) (timeSeconds / 60));
            weekly.add(entry);
        }
        return weekly;
    }

    private List<Map<String, Object>> buildRecentAchievements(User user) {
        List<Map<String, Object>> recent = new ArrayList<>();
        List<UserAchievement> unlocked = userAchievementRepository.findByUserIdOrderByUnlockedAtDesc(user.getId());
        for (int i = 0; i < Math.min(3, unlocked.size()); i++) {
            UserAchievement ua = unlocked.get(i);
            Achievement achievement = ua.getAchievement();
            Map<String, Object> entry = new HashMap<>();
            entry.put("name", achievement.getName());
            entry.put("icon", achievement.getIcon());
            entry.put("date", ua.getUnlockedAt() != null
                    ? ua.getUnlockedAt().toLocalDate().toString()
                    : "");
            recent.add(entry);
        }
        return recent;
    }

    private String dayLabel(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "Seg";
            case TUESDAY -> "Ter";
            case WEDNESDAY -> "Qua";
            case THURSDAY -> "Qui";
            case FRIDAY -> "Sex";
            case SATURDAY -> "Sáb";
            case SUNDAY -> "Dom";
        };
    }
}
