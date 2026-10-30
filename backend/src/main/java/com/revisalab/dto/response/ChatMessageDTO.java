package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Item do histórico de chat (uma mensagem da conversa).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private String role;          // "user" | "ai"
    private String content;
    private LocalDateTime timestamp;
}
