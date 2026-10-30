package com.revisalab.mapper;

import com.revisalab.dto.response.TrailDTO;
import com.revisalab.dto.response.TrailDetailDTO;
import com.revisalab.dto.response.ModuleDTO;
import com.revisalab.model.Trail;
import com.revisalab.model.Module;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mapper para conversões entre entidades Trail/Module e seus DTOs.
 *
 * O progresso é calculado externamente (com base nas respostas do usuário) e
 * recebido como parâmetro, mantendo o mapper puro e sem acesso a repositórios.
 */
@Component
public class TrailMapper {

    /**
     * Converte Trail para TrailDTO com progresso calculado.
     */
    public TrailDTO toDTO(Trail trail, long completedExercises) {
        if (trail == null) {
            return null;
        }

        int total = trail.getTotalExercises() == null ? 0 : trail.getTotalExercises();

        return TrailDTO.builder()
            .id(trail.getId())
            .title(trail.getTitle())
            .description(trail.getDescription())
            .category(trail.getCategory())
            .difficulty(trail.getDifficulty())
            .icon(trail.getIcon())
            .color(trail.getColor())
            .totalModules(trail.getTotalModules())
            .totalExercises(total)
            .estimatedHours(trail.getEstimatedHours())
            .progress(calculateProgress(completedExercises, total))
            .completedExercises((int) completedExercises)
            .build();
    }

    /**
     * Converte Trail para TrailDetailDTO com módulos e progresso calculado.
     */
    public TrailDetailDTO toDetailDTO(Trail trail, List<Module> modules,
                                      Map<Long, Long> moduleCorrectCounts,
                                      long trailCompletedExercises) {
        if (trail == null) {
            return null;
        }

        int total = trail.getTotalExercises() == null ? 0 : trail.getTotalExercises();
        List<ModuleDTO> moduleDTOs = toModuleDTOList(modules, moduleCorrectCounts);
        ModuleDTO currentModule = findCurrentModule(moduleDTOs);

        return TrailDetailDTO.builder()
            .id(trail.getId())
            .title(trail.getTitle())
            .description(trail.getDescription())
            .category(trail.getCategory())
            .difficulty(trail.getDifficulty())
            .icon(trail.getIcon())
            .color(trail.getColor())
            .totalModules(trail.getTotalModules())
            .totalExercises(total)
            .estimatedHours(trail.getEstimatedHours())
            .progress(calculateProgress(trailCompletedExercises, total))
            .completedExercises((int) trailCompletedExercises)
            .createdAt(trail.getCreatedAt())
            .updatedAt(trail.getUpdatedAt())
            .modules(moduleDTOs)
            .currentModuleOrder(currentModule != null ? currentModule.getOrderNumber() : 1)
            .currentModuleTitle(currentModule != null ? currentModule.getTitle() : null)
            .build();
    }

    /**
     * Identifica a etapa atual da trilha: o primeiro módulo ainda não concluído.
     * Quando todos estão concluídos, aponta para o último (trilha finalizada).
     * Os módulos chegam ordenados por {@code orderNumber}.
     */
    private ModuleDTO findCurrentModule(List<ModuleDTO> modules) {
        if (modules == null || modules.isEmpty()) {
            return null;
        }
        return modules.stream()
                .filter(m -> !Boolean.TRUE.equals(m.getCompleted()))
                .findFirst()
                .orElse(modules.get(modules.size() - 1));
    }

    /**
     * Converte Module para ModuleDTO com status de conclusão calculado.
     */
    public ModuleDTO toModuleDTO(Module module, long correctExercises) {
        if (module == null) {
            return null;
        }

        int total = module.getTotalExercises() == null ? 0 : module.getTotalExercises();
        boolean completed = total > 0 && correctExercises >= total;

        return ModuleDTO.builder()
            .id(module.getId())
            .title(module.getTitle())
            .description(module.getDescription())
            .orderNumber(module.getOrderNumber())
            .totalExercises(total)
            .estimatedMinutes(module.getEstimatedMinutes())
            .completed(completed)
            .build();
    }

    private List<ModuleDTO> toModuleDTOList(List<Module> modules, Map<Long, Long> moduleCorrectCounts) {
        if (modules == null) {
            return List.of();
        }

        return modules.stream()
            .map(module -> toModuleDTO(module,
                    moduleCorrectCounts.getOrDefault(module.getId(), 0L)))
            .toList();
    }

    private Double calculateProgress(long completedExercises, int totalExercises) {
        if (totalExercises <= 0) {
            return 0.0;
        }
        double progress = (double) completedExercises / totalExercises;
        return Math.min(1.0, Math.max(0.0, progress));
    }
}
