package com.revisalab.mapper;

import com.revisalab.dto.response.ChatResponse;
import com.revisalab.model.User;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversões relacionadas a chat.
 * Responsabilidade: converter dados de domínio em DTOs de resposta.
 * Regras de negócio (XP, validações) pertencem aos serviços.
 */
@Component
public class ChatMapper {

    public ChatResponse toChatResponse(User user, String question, String answer, int xpEarned,
                                       String source, String model, String context,
                                       boolean success, boolean usedAi, long responseTime) {
        return ChatResponse.builder()
                .question(question)
                .answer(answer)
                .xpEarned(xpEarned)
                .userXp(user != null ? user.getXp() : null)
                .userLevel(user != null ? user.getLevel() : null)
                .source(source)
                .model(model)
                .context(context)
                .success(success)
                .errorMessage(null)
                .usedAi(usedAi)
                .responseTime(responseTime)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public ChatResponse toErrorResponse(String errorMessage) {
        return ChatResponse.builder()
                .question(null)
                .answer("Desculpe, ocorreu um erro. Tente novamente.")
                .xpEarned(0)
                .userXp(null)
                .userLevel(null)
                .source("error")
                .model(null)
                .context(null)
                .success(false)
                .errorMessage(errorMessage)
                .usedAi(false)
                .responseTime(0L)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
