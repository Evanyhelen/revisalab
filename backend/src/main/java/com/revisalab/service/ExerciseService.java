package com.revisalab.service;

import com.revisalab.dto.response.ExerciseDTO;
import com.revisalab.dto.response.ExerciseHistoryDTO;
import com.revisalab.dto.response.ExerciseStatsDTO;
import com.revisalab.dto.response.ExerciseValidationResult;
import com.revisalab.mapper.ExerciseMapper;
import com.revisalab.model.Exercise;
import com.revisalab.model.StudySession;
import com.revisalab.model.User;
import com.revisalab.model.UserAnswer;
import com.revisalab.repository.ExerciseRepository;
import com.revisalab.repository.StudySessionRepository;
import com.revisalab.repository.UserRepository;
import com.revisalab.repository.UserAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de Gerenciamento de Exercícios.
 * Responsabilidades: recuperar exercícios, validar respostas, persistir o
 * histórico de respostas e calcular XP.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ExerciseService {

    private static final int XP_PER_CORRECT_EXERCISE = 10;

    // Teto diário de XP de sessões de quiz (ENEM/mock) reportadas pelo cliente.
    private static final int MAX_DAILY_SESSION_XP = 200;

    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final StudySessionRepository studySessionRepository;
    private final GamificationService gamificationService;
    private final ExerciseMapper exerciseMapper;

    public List<ExerciseDTO> getExercisesByTrail(Long trailId) {
        return exerciseMapper.toDTOList(exerciseRepository.findByModuleTrailId(trailId));
    }

    public List<ExerciseDTO> getExercisesByCategory(String category, int limit) {
        return exerciseMapper.toDTOList(exerciseRepository.findByCategory(category, PageRequest.of(0, limit)));
    }

    public List<ExerciseDTO> getRandomExercises(int count) {
        return exerciseMapper.toDTOList(exerciseRepository.findRandom(count));
    }

    /**
     * Valida a resposta do usuário e calcula XP.
     */
    public ExerciseValidationResult submitAnswer(Long exerciseId, String userAnswer, Long userId, Integer timeSpent) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new IllegalArgumentException("Exercício não encontrado: " + exerciseId));

        boolean isCorrect = validateAnswer(userAnswer, exercise.getCorrectAnswer());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        // O usuário ganha XP apenas a PRIMEIRA vez que acerta um exercício.
        // Reenviar uma resposta correta já registrada não gera XP novamente
        // (evita "farm" re-submetendo a mesma resposta).
        int xpEarned = 0;
        if (isCorrect && userAnswerRepository.countCorrectAnswersByUserAndExercise(user, exercise) == 0) {
            xpEarned = XP_PER_CORRECT_EXERCISE;
        }

        // Responder um exercício conta como atividade diária (mantém o streak),
        // independentemente de acerto ou erro.
        user.registerDailyActivity();

        if (xpEarned > 0) {
            user.addXp(xpEarned);
        }
        // Avalia conquistas SEMPRE: o streak avança mesmo em respostas incorretas
        // (registerDailyActivity), então STREAK_3/STREAK_7 não podem depender de
        // um acerto para serem desbloqueadas no dia em que a meta é atingida.
        gamificationService.checkAndUnlockAchievements(user);
        userRepository.save(user);

        // Persiste o histórico da resposta (fonte de verdade para progresso e estatísticas).
        userAnswerRepository.save(UserAnswer.builder()
                .user(user)
                .exercise(exercise)
                .answer(userAnswer)
                .isCorrect(isCorrect)
                .submittedAt(LocalDateTime.now())
                .timeSpentSeconds(timeSpent != null ? Math.max(0, timeSpent) : null)
                .attemptsCount(1)
                .xpEarned(xpEarned)
                .build());

        return ExerciseValidationResult.builder()
                .correct(isCorrect)
                .explanation(exercise.getExplanation())
                .correctAnswer(exercise.getCorrectAnswer())
                .xpEarned(xpEarned)
                .userXp(user.getXp())
                .userLevel(user.getLevel())
                .userStreak(user.getStreak())
                .build();
    }

    /**
     * Registra o XP de uma sessão de quiz composta por questões simuladas/ENEM,
     * que não passam por /submit individual (mesma semântica do Construtor de Base:
     * o total é fornecido pelo cliente e limitado para evitar abuso).
     *
     * <p>Além do XP, persiste um {@link StudySession} para que a atividade também
     * apareça no painel de desempenho (estatísticas e histórico), e não apenas no
     * XP/level/streak do usuário.</p>
     */
    public Map<String, Object> completeSession(Long userId, int xp,
                                               int correctAnswers, int totalQuestions, String category,
                                               int durationSeconds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        int cappedXp = grantSessionXp(user, Math.max(0, Math.min(xp, 200)));
        if (cappedXp > 0) {
            user.addXp(cappedXp);
        }
        user.registerDailyActivity();
        gamificationService.checkAndUnlockAchievements(user);
        userRepository.save(user);

        // Registra a sessão quando há questões respondidas (fonte para o desempenho).
        int safeTotal = Math.max(0, totalQuestions);
        if (safeTotal > 0) {
            int safeCorrect = Math.min(Math.max(correctAnswers, 0), safeTotal);
            studySessionRepository.save(StudySession.builder()
                    .user(user)
                    .category(category == null || category.isBlank() ? "Prática" : category)
                    .correctAnswers(safeCorrect)
                    .totalQuestions(safeTotal)
                    .xpEarned(cappedXp)
                    .durationSeconds(Math.max(0, durationSeconds))
                    .completedAt(LocalDateTime.now())
                    .build());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("xpEarned", cappedXp);
        result.put("userXp", user.getXp());
        result.put("userLevel", user.getLevel());
        result.put("userStreak", user.getStreak());
        return result;
    }

    /**
     * Obtém o histórico de respostas do usuário, mais recentes primeiro.
     * Inclui também as sessões de estudo (ENEM/Construtor) como itens agregados.
     */
    public List<ExerciseHistoryDTO> getHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        List<ExerciseHistoryDTO> history = new ArrayList<>(userAnswerRepository.findByUser(user).stream()
                .sorted(Comparator.comparing(UserAnswer::getSubmittedAt).reversed())
                .map(answer -> ExerciseHistoryDTO.builder()
                        .id(answer.getId())
                        .question(answer.getExercise().getQuestion())
                        .category(categoryOf(answer.getExercise()))
                        .correct(Boolean.TRUE.equals(answer.getIsCorrect()))
                        .timestamp(answer.getSubmittedAt())
                        .xpEarned(answer.getXpEarned() != null
                                ? answer.getXpEarned()
                                : (Boolean.TRUE.equals(answer.getIsCorrect()) ? XP_PER_CORRECT_EXERCISE : 0))
                        .build())
                .toList());

        studySessionRepository.findByUser(user).forEach(session ->
                history.add(ExerciseHistoryDTO.builder()
                        .id(session.getId())
                        .question("Sessão de " + session.getCategory())
                        .category(session.getCategory())
                        .correct(session.getCorrectAnswers() > 0)
                        .timestamp(session.getCompletedAt())
                        .xpEarned(session.getXpEarned())
                        .isSession(true)
                        .correctAnswers(session.getCorrectAnswers())
                        .totalQuestions(session.getTotalQuestions())
                        .build()));

        history.sort(Comparator.comparing(
                ExerciseHistoryDTO::getTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return history;
    }

    /**
     * Obtém as estatísticas de exercícios do usuário.
     *
     * <p>Agrega tanto as respostas a exercícios individuais ({@link UserAnswer})
     * quanto as sessões de estudo ({@link StudySession}) concluídas via quiz do
     * ENEM e Construtor de Base — assim o desempenho reflete toda a prática.</p>
     */
    public ExerciseStatsDTO getStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        List<UserAnswer> answers = userAnswerRepository.findByUser(user);
        List<StudySession> sessions = studySessionRepository.findByUser(user);

        long answerCorrect = answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        long sessionCorrect = sessions.stream().mapToLong(StudySession::getCorrectAnswers).sum();
        long sessionTotal = sessions.stream().mapToLong(StudySession::getTotalQuestions).sum();

        long total = answers.size() + sessionTotal;
        long correct = answerCorrect + sessionCorrect;
        long incorrect = total - correct;
        double accuracy = total > 0 ? (correct * 100.0) / total : 0.0;
        // XP real registrado por resposta (registros antigos sem xpEarned usam a regra padrão).
        int answerXp = answers.stream()
                .mapToInt(a -> a.getXpEarned() != null
                        ? a.getXpEarned()
                        : (Boolean.TRUE.equals(a.getIsCorrect()) ? XP_PER_CORRECT_EXERCISE : 0))
                .sum();
        int totalXp = answerXp + sessions.stream().mapToInt(StudySession::getXpEarned).sum();

        Map<String, ExerciseStatsDTO.CategoryStat> categoryStats = new HashMap<>();
        for (UserAnswer answer : answers) {
            String category = categoryOf(answer.getExercise());
            ExerciseStatsDTO.CategoryStat stat = categoryStats.computeIfAbsent(category,
                    k -> ExerciseStatsDTO.CategoryStat.builder().build());
            stat.setTotal(stat.getTotal() + 1);
            if (Boolean.TRUE.equals(answer.getIsCorrect())) {
                stat.setCorrect(stat.getCorrect() + 1);
            }
        }
        for (StudySession session : sessions) {
            String category = session.getCategory() != null && !session.getCategory().isBlank()
                    ? session.getCategory() : "Prática";
            ExerciseStatsDTO.CategoryStat stat = categoryStats.computeIfAbsent(category,
                    k -> ExerciseStatsDTO.CategoryStat.builder().build());
            stat.setTotal(stat.getTotal() + session.getTotalQuestions());
            stat.setCorrect(stat.getCorrect() + session.getCorrectAnswers());
        }
        categoryStats.values().forEach(stat ->
                stat.setAccuracy(stat.getTotal() > 0
                        ? (stat.getCorrect() * 100.0) / stat.getTotal()
                        : 0.0));

        long totalTimeSeconds = answers.stream()
                .filter(a -> a.getTimeSpentSeconds() != null)
                .mapToLong(UserAnswer::getTimeSpentSeconds).sum()
                + sessions.stream()
                        .filter(s -> s.getDurationSeconds() != null)
                        .mapToLong(StudySession::getDurationSeconds).sum();
        int averageTime = total > 0 ? (int) (totalTimeSeconds / total) : 0;

        return ExerciseStatsDTO.builder()
                .totalExercises(total)
                .correctAnswers(correct)
                .incorrectAnswers(incorrect)
                .accuracyRate(Math.round(accuracy * 10.0) / 10.0)
                .totalXpEarned(totalXp)
                .averageTimePerExercise(averageTime)
                .categoryStats(categoryStats)
                .build();
    }

    private String categoryOf(Exercise exercise) {
        if (exercise.getModule() != null && exercise.getModule().getTrail() != null) {
            return exercise.getModule().getTrail().getCategory();
        }
        return "Geral";
    }

    /**
     * Concede XP de sessão de quiz respeitando o teto diário e atualiza o contador
     * do usuário. Retorna o XP efetivamente concedido (0 quando o teto foi atingido).
     */
    private int grantSessionXp(User user, int requested) {
        LocalDate today = LocalDate.now();
        int usedToday = 0;
        if (today.equals(user.getLastSessionXpDate())) {
            usedToday = user.getDailySessionXp() == null ? 0 : user.getDailySessionXp();
        }

        int remaining = MAX_DAILY_SESSION_XP - usedToday;
        if (remaining <= 0) {
            return 0;
        }

        int granted = Math.min(requested, remaining);
        user.setLastSessionXpDate(today);
        user.setDailySessionXp(usedToday + granted);
        return granted;
    }

    /**
     * Comparação case-insensitive, com trim de espaços.
     */
    protected boolean validateAnswer(String userAnswer, String correctAnswer) {
        return userAnswer != null
                && correctAnswer != null
                && userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
    }
}
