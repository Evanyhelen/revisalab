package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de chat com IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String question;
    private String answer;
    private int xpEarned;
    private Integer userXp;
    private Integer userLevel;
    private String source;
    private String model;
    private String context;
    private boolean success;
    private String errorMessage;
    private boolean usedAi;
    private long responseTime;
    private long timestamp;
}
