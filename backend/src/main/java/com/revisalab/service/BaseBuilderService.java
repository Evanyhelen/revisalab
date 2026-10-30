package com.revisalab.service;

import com.revisalab.model.StudySession;
import com.revisalab.model.User;
import com.revisalab.repository.StudySessionRepository;
import com.revisalab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de Construtor de Base (RF10)
 * 
 * Responsabilidades:
 * - Gerenciar estado do construtor de base
 * - Validar construções
 * - Calcular XP por blocos adicionados
 * - Rastrear progresso de construção
 * 
 * Conceito:
 * Cada bloco adicionado corretamente constrói uma "base" visual,
 * reforçando conceitos fundamentais através de gamificação visual.
 * 
 * Princípios SOLID:
 * - Single Responsibility: Apenas lógica de construtor
 * - Open/Closed: Fácil adicionar novos tipos de blocos
 */
@Service
@Transactional
public class BaseBuilderService {

    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final StudySessionRepository studySessionRepository;

    // Constantes
    private static final int XP_PER_CORRECT_BLOCK = 15;
    private static final int MAX_BLOCKS = 50;

    // Limite diário de XP do construtor, para impedir "farm" de XP enviando
    // requisições repetidas de /builder/complete com acertos arbitrários.
    private static final int MAX_DAILY_BUILDER_XP = 750;

    @Autowired
    public BaseBuilderService(UserRepository userRepository, GamificationService gamificationService,
                              StudySessionRepository studySessionRepository) {
        this.userRepository = userRepository;
        this.gamificationService = gamificationService;
        this.studySessionRepository = studySessionRepository;
    }

    /**
     * Obtém o estado inicial do construtor para um usuário
     * @param userId ID do usuário
     * @return Estado do construtor com blocos iniciais
     */
    public BaseBuilderStateDTO getInitialState(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        List<BuildingBlockDTO> blocks = generateInitialBlocks();

        return new BaseBuilderStateDTO(
            user.getId(),
            "Construtor de Fundamentos",
            "Complete os desafios para construir sua base de conhecimento!",
            blocks,
            0,
            0
        );
    }

    /**
     * Adiciona um bloco e valida se está correto
     * @param userId ID do usuário
     * @param blockRequest Bloco a adicionar
     * @return Resultado da adição
     */
    public AddBlockResultDTO addBlock(Long userId, AddBlockRequestDTO blockRequest) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        // Validar se resposta está correta
        boolean isCorrect = validateBlockAnswer(blockRequest);

        int xpEarned = 0;

        if (isCorrect) {
            xpEarned = XP_PER_CORRECT_BLOCK;
            user.addXp(xpEarned);
            userRepository.save(user);
        }

        return new AddBlockResultDTO(
            isCorrect,
            blockRequest.getBlockType(),
            xpEarned,
            user.getXp(),
            user.getLevel(),
            isCorrect ? "Bloco adicionado com sucesso!" : "Resposta incorreta, tente novamente."
        );
    }

    /**
     * Remove um bloco da construção
     * @param userId ID do usuário
     * @param blockId ID do bloco a remover
     * @return Resultado da remoção
     */
    public RemoveBlockResultDTO removeBlock(Long userId, Long blockId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        return new RemoveBlockResultDTO(
            blockId,
            true,
            "Bloco removido com sucesso",
            user.getXp()
        );
    }

    /**
     * Obtém progresso atual da construção
     * @param userId ID do usuário
     * @return Dados de progresso
     */
    public BuildProgressDTO getBuildProgress(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        return new BuildProgressDTO(
            user.getId(),
            0, // Blocos colocados (simulado)
            MAX_BLOCKS,
            0.0, // Percentual de conclusão
            user.getXp(),
            user.getLevel()
        );
    }

    /**
     * Conclui a sessão do construtor, concedendo XP proporcional aos blocos
     * respondidos corretamente. Persiste o XP do usuário e registra a sessão
     * para que ela apareça no painel de desempenho.
     *
     * @param userId ID do usuário
     * @param correctAnswers quantidade de respostas corretas na sessão
     * @param totalQuestions total de questões da sessão
     * @return resumo do XP concedido e estado atual do usuário
     */
    public Map<String, Object> completeSession(Long userId, int correctAnswers, int totalQuestions,
                                               int durationSeconds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        // Protege contra inflação de XP: um cliente pode reportar um número de
        // acertos arbitrário. Limita o total de questões ao máximo de uma sessão
        // e os acertos ao próprio total de questões (não faz sentido acertar mais
        // do que o número de questões respondidas).
        int safeTotal = Math.min(Math.max(totalQuestions, 0), MAX_BLOCKS);
        int cappedAnswers = Math.min(Math.max(correctAnswers, 0), safeTotal);

        // Concede XP respeitando o teto diário do construtor.
        int xpEarned = grantBuilderXp(user, cappedAnswers * XP_PER_CORRECT_BLOCK);
        if (xpEarned > 0) {
            user.addXp(xpEarned);
        }

        // Jogar o construtor também conta como atividade diária (mantém o streak).
        user.registerDailyActivity();
        // Conquistas (STREAK_3, STREAK_7, LEVEL_5, EXERCISES_50) também devem ser
        // avaliadas aqui — senão quem só joga o Construtor nunca as desbloqueia.
        gamificationService.checkAndUnlockAchievements(user);
        userRepository.save(user);

        // Registra a sessão para o painel de desempenho.
        if (safeTotal > 0) {
            studySessionRepository.save(StudySession.builder()
                    .user(user)
                    .category("Construtor de Base")
                    .correctAnswers(cappedAnswers)
                    .totalQuestions(safeTotal)
                    .xpEarned(xpEarned)
                    .durationSeconds(Math.max(0, durationSeconds))
                    .completedAt(java.time.LocalDateTime.now())
                    .build());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("xpEarned", xpEarned);
        result.put("userXp", user.getXp());
        result.put("userLevel", user.getLevel());
        result.put("userStreak", user.getStreak());
        return result;
    }

    /**
     * Concede XP do construtor respeitando o teto diário e atualiza o contador do
     * usuário. Retorna o XP efetivamente concedido (0 quando o teto diário já foi
     * atingido).
     */
    private int grantBuilderXp(User user, int requested) {
        LocalDate today = LocalDate.now();
        int usedToday = 0;
        if (today.equals(user.getLastBuilderXpDate())) {
            usedToday = user.getDailyBuilderXp() == null ? 0 : user.getDailyBuilderXp();
        }

        int remaining = MAX_DAILY_BUILDER_XP - usedToday;
        if (remaining <= 0) {
            return 0;
        }

        int granted = Math.min(requested, remaining);
        user.setLastBuilderXpDate(today);
        user.setDailyBuilderXp(usedToday + granted);
        return granted;
    }

    /**
     * Valida resposta de um bloco
     * @param blockRequest Bloco com resposta
     * @return true se resposta está correta
     */
    private boolean validateBlockAnswer(AddBlockRequestDTO blockRequest) {
        // Implementar validação conforme tipo de bloco
        switch (blockRequest.getBlockType()) {
            case "math":
                return validateMathBlock(blockRequest.getAnswer());
            case "language":
                return validateLanguageBlock(blockRequest.getAnswer());
            case "science":
                return validateScienceBlock(blockRequest.getAnswer());
            default:
                return false;
        }
    }

    private boolean validateMathBlock(String answer) {
        // Simplificado - em produção ter validação real
        return answer != null && !answer.trim().isEmpty();
    }

    private boolean validateLanguageBlock(String answer) {
        return answer != null && !answer.trim().isEmpty();
    }

    private boolean validateScienceBlock(String answer) {
        return answer != null && !answer.trim().isEmpty();
    }

    /**
     * Gera blocos iniciais para construtor
     */
    private List<BuildingBlockDTO> generateInitialBlocks() {
        List<BuildingBlockDTO> blocks = new ArrayList<>();

        blocks.add(new BuildingBlockDTO(
            1L, "math", "Resolva: 2 + 2 = ?",
            "Bloco de Matemática", "calculate", "#3f51b5"
        ));

        blocks.add(new BuildingBlockDTO(
            2L, "language", "Complete: A palavra 'beleza' é um _____",
            "Bloco de Português", "edit_note", "#f44336"
        ));

        blocks.add(new BuildingBlockDTO(
            3L, "science", "Qual é a fórmula da velocidade?",
            "Bloco de Física", "science", "#4caf50"
        ));

        return blocks;
    }

    // DTOs para requisição e resposta
    public static class BaseBuilderStateDTO {
        public Long userId;
        public String title;
        public String description;
        public List<BuildingBlockDTO> availableBlocks;
        public int blocksPlaced;
        public int totalXp;

        public BaseBuilderStateDTO(Long userId, String title, String description,
                                 List<BuildingBlockDTO> availableBlocks,
                                 int blocksPlaced, int totalXp) {
            this.userId = userId;
            this.title = title;
            this.description = description;
            this.availableBlocks = availableBlocks;
            this.blocksPlaced = blocksPlaced;
            this.totalXp = totalXp;
        }
    }

    public static class BuildingBlockDTO {
        public Long id;
        public String type;
        public String question;
        public String label;
        public String icon;
        public String color;

        public BuildingBlockDTO(Long id, String type, String question,
                               String label, String icon, String color) {
            this.id = id;
            this.type = type;
            this.question = question;
            this.label = label;
            this.icon = icon;
            this.color = color;
        }
    }

    public static class AddBlockRequestDTO {
        private String blockType;
        private String answer;

        public String getBlockType() { return blockType; }
        public void setBlockType(String blockType) { this.blockType = blockType; }

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
    }

    public static class AddBlockResultDTO {
        public boolean correct;
        public String blockType;
        public int xpEarned;
        public int userXp;
        public int userLevel;
        public String message;

        public AddBlockResultDTO(boolean correct, String blockType, int xpEarned,
                                int userXp, int userLevel, String message) {
            this.correct = correct;
            this.blockType = blockType;
            this.xpEarned = xpEarned;
            this.userXp = userXp;
            this.userLevel = userLevel;
            this.message = message;
        }
    }

    public static class RemoveBlockResultDTO {
        public Long blockId;
        public boolean success;
        public String message;
        public int userXp;

        public RemoveBlockResultDTO(Long blockId, boolean success, String message, int userXp) {
            this.blockId = blockId;
            this.success = success;
            this.message = message;
            this.userXp = userXp;
        }
    }

    public static class BuildProgressDTO {
        public Long userId;
        public int blocksPlaced;
        public int totalBlocks;
        public double progressPercentage;
        public int userXp;
        public int userLevel;

        public BuildProgressDTO(Long userId, int blocksPlaced, int totalBlocks,
                               double progressPercentage, int userXp, int userLevel) {
            this.userId = userId;
            this.blocksPlaced = blocksPlaced;
            this.totalBlocks = totalBlocks;
            this.progressPercentage = progressPercentage;
            this.userXp = userXp;
            this.userLevel = userLevel;
        }
    }
}
