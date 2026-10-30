package com.revisalab.service;

import com.revisalab.dto.response.TrailDTO;
import com.revisalab.dto.response.TrailDetailDTO;
import com.revisalab.mapper.TrailMapper;
import com.revisalab.model.Trail;
import com.revisalab.model.Module;
import com.revisalab.repository.TrailRepository;
import com.revisalab.repository.ModuleRepository;
import com.revisalab.repository.UserAnswerRepository;
import com.revisalab.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço de Gerenciamento de Trilhas.
 *
 * Responsabilidades:
 * - Recuperar trilhas e módulos.
 * - Calcular o progresso real de cada trilha/módulo com base nas respostas do usuário.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TrailService {

    private final TrailRepository trailRepository;
    private final ModuleRepository moduleRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final StudySessionRepository studySessionRepository;
    private final TrailMapper trailMapper;

    /**
     * Obtém todas as trilhas com progresso do usuário.
     */
    public List<TrailDTO> getAllTrails(Long userId) {
        return trailRepository.findAll().stream()
            .map(trail -> toDTO(trail, userId))
            .toList();
    }

    /**
     * Obtém trilhas por categoria com progresso do usuário.
     */
    public List<TrailDTO> getTrailsByCategory(String category, Long userId) {
        return trailRepository.findByCategory(category).stream()
            .map(trail -> toDTO(trail, userId))
            .toList();
    }

    /**
     * Obtém trilha por ID com módulos e progresso do usuário.
     */
    public TrailDetailDTO getTrailById(Long trailId, Long userId) {
        Trail trail = trailRepository.findById(trailId)
            .orElseThrow(() -> new IllegalArgumentException("Trilha não encontrada: " + trailId));

        List<Module> modules = moduleRepository.findByTrailIdOrderByOrderNumber(trailId);

        Map<Long, Long> moduleCorrectCounts = new HashMap<>();
        long trailCompletedExercises = 0L;

        for (Module module : modules) {
            long correct = userAnswerRepository
                    .countDistinctCorrectExercisesForUserAndModule(userId, module.getId());
            moduleCorrectCounts.put(module.getId(), correct);
            trailCompletedExercises += correct;
        }

        // Se por algum motivo o mapeamento de módulos não cobrir tudo, usa a consulta agregada.
        if (trailCompletedExercises == 0) {
            trailCompletedExercises = userAnswerRepository
                    .countDistinctCorrectExercisesForUserAndTrail(userId, trailId);
        }

        // Credita também a prática pelo ENEM (sessões da mesma categoria) no
        // progresso geral da trilha, sem alterar a contagem por módulo.
        trailCompletedExercises += studySessionRepository
                .sumCorrectAnswersByUserAndCategory(userId, trail.getCategory());
        long totalExercises = trail.getTotalExercises() == null ? 0L : trail.getTotalExercises();
        trailCompletedExercises = Math.min(trailCompletedExercises, totalExercises);

        return trailMapper.toDetailDTO(trail, modules, moduleCorrectCounts, trailCompletedExercises);
    }

    /**
     * Obtém categorias disponíveis.
     */
    public Map<String, Long> getCategories() {
        return trailRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                Trail::getCategory,
                Collectors.counting()
            ));
    }

    private TrailDTO toDTO(Trail trail, Long userId) {
        return trailMapper.toDTO(trail, completedExercisesForTrail(trail, userId));
    }

    /**
     * Total de itens concluídos numa trilha: exercícios próprios respondidos
     * corretamente + acertos em sessões de prática (ENEM) da mesma categoria,
     * limitado ao total de exercícios da trilha para nunca ultrapassar 100%.
     */
    private long completedExercisesForTrail(Trail trail, Long userId) {
        long completed = userAnswerRepository
                .countDistinctCorrectExercisesForUserAndTrail(userId, trail.getId());
        completed += studySessionRepository
                .sumCorrectAnswersByUserAndCategory(userId, trail.getCategory());
        long total = trail.getTotalExercises() == null ? 0L : trail.getTotalExercises();
        return Math.min(completed, total);
    }
}
