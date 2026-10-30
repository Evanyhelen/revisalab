package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para representação detalhada de uma trilha com módulos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrailDetailDTO {
    private Long id;
    private String title;
    private String description;
    private String category;
    private Integer difficulty;
    private String icon;
    private String color;
    private Integer totalModules;
    private Integer totalExercises;
    private Integer estimatedHours;
    private Double progress;
    private Integer completedExercises;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ModuleDTO> modules;

    /** Etapa atual: primeiro módulo ainda não concluído (1-based, pela orderNumber). */
    private Integer currentModuleOrder;

    /** Título da etapa atual (ajuda o front a rotular "Continuar no Módulo X"). */
    private String currentModuleTitle;
}