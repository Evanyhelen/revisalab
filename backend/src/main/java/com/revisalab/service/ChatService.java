package com.revisalab.service;

import com.revisalab.dto.response.ChatResponse;
import com.revisalab.dto.response.ChatMessageDTO;
import com.revisalab.mapper.ChatMapper;
import com.revisalab.model.ChatMessage;
import com.revisalab.model.User;
import com.revisalab.repository.UserRepository;
import com.revisalab.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Serviço de Chat com IA.
 *
 * Responsabilidades:
 * - Orquestrar a resposta: primeiro tenta a IA (Groq), depois a base local.
 * - Conceder XP e persistir o progresso do usuário.
 * - Persistir o histórico da conversa para que ela sobreviva a recargas.
 *
 * A comunicação HTTP com a Groq é delegada ao {@link AiService}, mantendo este
 * serviço focado apenas na regra de negócio do chat (single responsibility).
 */
@Service
public class ChatService {

    private static final int XP_PER_AI_QUESTION = 5;
    private static final int XP_PER_FALLBACK_QUESTION = 2;

    // Limite diário de XP obtido pelo chat, para impedir "farm" de XP com
    // perguntas repetidas (ex.: contas aritméticas) e preservar a economia
    // de XP/nível do ranking.
    private static final int MAX_DAILY_CHAT_XP = 30;

    private final UserRepository userRepository;
    private final AiService aiService;
    private final LocalKnowledgeBase localKnowledgeBase;
    private final ArithmeticService arithmeticService;
    private final ChatMapper chatMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final GamificationService gamificationService;

    public ChatService(UserRepository userRepository,
                       AiService aiService,
                       LocalKnowledgeBase localKnowledgeBase,
                       ArithmeticService arithmeticService,
                       ChatMapper chatMapper,
                       ChatMessageRepository chatMessageRepository,
                       GamificationService gamificationService) {
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.localKnowledgeBase = localKnowledgeBase;
        this.arithmeticService = arithmeticService;
        this.chatMapper = chatMapper;
        this.chatMessageRepository = chatMessageRepository;
        this.gamificationService = gamificationService;
    }

    /**
     * Processa a pergunta do usuário, tentando a IA primeiro e a base local como fallback.
     */
    public ChatResponse askQuestion(User user, String question, String context) {
        validateQuestion(question);

        long startTime = System.currentTimeMillis();

        String answer;
        boolean usedAi;
        String source;
        String model;
        int baseXp;

        // Aritmética simples é resolvida localmente (exata e instantânea), evitando
        // que a IA erre contas — sobretudo com números muito grandes.
        String arithmeticAnswer = arithmeticService.tryEvaluate(question);
        if (arithmeticAnswer != null) {
            answer = arithmeticAnswer;
            usedAi = false;
            source = "Calculadora";
            model = "Aritmética local";
            baseXp = XP_PER_FALLBACK_QUESTION;
        } else {
            try {
                answer = aiService.getAiResponse(question);
                usedAi = true;
                source = "Groq AI";
                model = aiService.getModelName();
                baseXp = XP_PER_AI_QUESTION;
            } catch (Exception e) {
                // Sem chave configurada, sem rede ou qualquer falha da IA: usa a base local.
                answer = localKnowledgeBase.getAnswer(question, context);
                usedAi = false;
                source = "Local Knowledge Base";
                model = "Local KB";
                baseXp = XP_PER_FALLBACK_QUESTION;
            }
        }

        long responseTime = System.currentTimeMillis() - startTime;

        // Concede XP apenas quando há uma resposta real, respeitando o teto diário.
        // Obs.: não atualizamos lastActivity aqui — essa data é usada pelo
        // ExerciseService para controlar o streak diário (uma vez por dia).
        // Se o chat a atualizasse, uma conversa impediria o incremento do
        // streak ao responder um exercício no mesmo dia.
        int xpEarned = 0;
        if (answer != null && !answer.trim().isEmpty()) {
            xpEarned = grantChatXp(user, baseXp);
            if (xpEarned > 0) {
                user.addXp(xpEarned);
                // O XP do chat também pode cruzar o limite de um nível; sem esta
                // verificação, conquistas como "Mestre em Formação" (nível 5) só
                // seriam desbloqueadas num exercício/sessão posterior.
                gamificationService.checkAndUnlockAchievements(user);
                userRepository.save(user);
            }

            // Persiste a pergunta e a resposta para manter o histórico da conversa.
            chatMessageRepository.save(ChatMessage.builder()
                    .user(user).role("user").content(question).build());
            chatMessageRepository.save(ChatMessage.builder()
                    .user(user).role("ai").content(answer).build());
        }

        return chatMapper.toChatResponse(
                user, question, answer, xpEarned, source, model, context,
                true, usedAi, responseTime);
    }

    /**
     * Retorna o histórico da conversa do usuário (até as 50 mensagens mais
     * recentes), em ordem cronológica.
     */
    public List<ChatMessageDTO> getHistory(User user) {
        List<ChatMessage> recent = chatMessageRepository
                .findTop50ByUserOrderByCreatedAtDesc(user);
        List<ChatMessage> ordered = new java.util.ArrayList<>(recent);
        Collections.reverse(ordered);
        return ordered.stream()
                .map(m -> ChatMessageDTO.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .timestamp(m.getCreatedAt())
                        .build())
                .toList();
    }

    /** Apaga o histórico de conversa do usuário (botão "Nova conversa"). */
    @Transactional
    public void clearHistory(User user) {
        chatMessageRepository.deleteByUser(user);
    }

    /**
     * Concede XP de chat respeitando o teto diário e atualiza o contador do usuário.
     * Retorna o XP efetivamente concedido (0 quando o teto diário já foi atingido).
     */
    private int grantChatXp(User user, int requested) {
        LocalDate today = LocalDate.now();
        int usedToday = 0;
        if (today.equals(user.getLastChatXpDate())) {
            usedToday = user.getDailyChatXp() == null ? 0 : user.getDailyChatXp();
        }

        int remaining = MAX_DAILY_CHAT_XP - usedToday;
        if (remaining <= 0) {
            return 0;
        }

        int granted = Math.min(requested, remaining);
        user.setLastChatXpDate(today);
        user.setDailyChatXp(usedToday + granted);
        return granted;
    }

    private void validateQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Pergunta não pode ser vazia");
        }
        if (question.length() > 500) {
            throw new IllegalArgumentException("Pergunta muito longa (máximo 500 caracteres)");
        }
    }
}
