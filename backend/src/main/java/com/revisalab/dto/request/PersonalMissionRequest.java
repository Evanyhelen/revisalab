package com.revisalab.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Requisição para criação de uma missão pessoal.
 *
 * As metas (requiredValue) e recompensas (xpReward) são validadas no backend
 * para evitar objetivos impossíveis ou inflação de XP. O actionType deve ser
 * um dos tipos mensuráveis a partir dos dados persistidos.
 */
@Data
public class PersonalMissionRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 100, message = "Título deve ter no máximo 100 caracteres")
    private String title;

    @Size(max = 300, message = "Descrição deve ter no máximo 300 caracteres")
    private String description;

    @NotBlank(message = "Periodicidade é obrigatória")
    @Pattern(regexp = "daily|weekly", message = "Periodicidade deve ser 'daily' ou 'weekly'")
    private String type;

    @NotBlank(message = "Tipo de meta é obrigatório")
    @Pattern(regexp = "complete_exercises|study_minutes|streak",
            message = "Tipo de meta inválido")
    private String actionType;

    @NotNull(message = "Meta é obrigatória")
    @Min(value = 1, message = "Meta mínima é 1")
    @Max(value = 500, message = "Meta máxima é 500")
    private Integer requiredValue;

    @NotNull(message = "Recompensa é obrigatória")
    @Min(value = 1, message = "Recompensa mínima é 1 XP")
    @Max(value = 200, message = "Recompensa máxima é 200 XP")
    private Integer xpReward;
}
