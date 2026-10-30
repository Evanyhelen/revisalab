package com.revisalab.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;
import java.util.*;

/**
 * Serviço de integração com provedores de IA (Groq, OpenRouter e Hugging Face).
 *
 * O provedor ativo é definido pela propriedade {@code ai.provider}
 * ({@code groq}, {@code openrouter} ou {@code huggingface}). Groq e OpenRouter
 * compartilham o mesmo formato OpenAI-compatible de chat completions; Hugging
 * Face usa o formato de Inference API.
 *
 * Em caso de falha (chave ausente, timeout ou erro da API), lança exceção para
 * que o {@link ChatService} possa aplicar o fallback local.
 */
@Service
public class AiService {
    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private static final String SYSTEM_PROMPT =
            "Você é um tutor educacional especializado em ensino médio brasileiro. " +
            "Forneça explicações claras, concisas e educativas em português do Brasil. " +
            "Use exemplos práticos e linguagem acessível. " +
            "Foco em: Matemática, Português, Física, Química, História, Geografia e Biologia. " +
            "Seja direto e objetivo: priorize respostas curtas, em tópicos/listas, e evite escrever " +
            "textos longos ou redações completas de exemplo. " +
            "Seja encorajador, motivador e paciente.";

    // A Groq (assim como OpenRouter) fica atrás do Cloudflare, que responde
    // 403/1010 para User-Agents não-browser (Java, Python, "RevisaLab/1.0", ...).
    // Um User-Agent de navegador é necessário para a API aceitar a chamada.
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    @Value("${ai.provider:groq}")
    private String provider;

    // ---- Groq ----
    @Value("${groq.api.key:}")
    private String groqKey;
    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqUrl;
    @Value("${groq.api.model:openai/gpt-oss-120b}")
    private String groqModel;

    // ---- OpenRouter ----
    @Value("${ai.openrouter.api.key:}")
    private String openRouterKey;
    @Value("${ai.openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String openRouterUrl;
    @Value("${ai.openrouter.api.model:google/gemini-2.0-flash-exp:free}")
    private String openRouterModel;

    // ---- Hugging Face ----
    @Value("${ai.huggingface.api.key:}")
    private String huggingFaceKey;
    @Value("${ai.huggingface.api.url:https://api-inference.huggingface.co/models}")
    private String huggingFaceUrl;
    @Value("${ai.huggingface.api.model:mistralai/Mistral-7B-Instruct-v0.3}")
    private String huggingFaceModel;

    private final RestTemplate restTemplate;

    public AiService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofSeconds(30))
            .build();
    }

    /**
     * Retorna o nome do provedor atualmente configurado.
     */
    public String getProviderName() {
        return provider == null ? "groq" : provider.trim().toLowerCase();
    }

    /**
     * Retorna o nome do modelo do provedor ativo.
     */
    public String getModelName() {
        return switch (getProviderName()) {
            case "openrouter" -> openRouterModel;
            case "huggingface" -> huggingFaceModel;
            default -> groqModel;
        };
    }

    /**
     * Obtém resposta da IA para uma pergunta do usuário.
     * Lança exceção se a API falhar - sem fallback local (o fallback fica no ChatService).
     */
    public String getAiResponse(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Pergunta não pode ser vazia");
        }
        question = question.trim();
        if (question.length() > 500) {
            throw new IllegalArgumentException("Pergunta muito longa (máximo 500 caracteres)");
        }

        return switch (getProviderName()) {
            case "openrouter" -> callOpenAiCompatible(
                    question, openRouterKey, openRouterUrl, openRouterModel,
                    List.of(openRouterModel, "google/gemini-2.0-flash-exp:free", "openai/gpt-4o-mini"),
                    "OpenRouter");
            case "huggingface" -> callHuggingFace(question);
            default -> callOpenAiCompatible(
                    question, groqKey, groqUrl, groqModel,
                    List.of(groqModel, "openai/gpt-oss-120b", "qwen/qwen3.8-27b", "openai/gpt-oss-20b"),
                    "Groq");
        };
    }

    /**
     * Chama um provedor com API compatível com OpenAI (Groq e OpenRouter).
     */
    private String callOpenAiCompatible(String question, String apiKey, String apiUrl,
                                        String model, List<String> modelFallbacks,
                                        String providerName) {
        validateKey(apiKey, providerName);

        String lastError = null;
        for (String currentModel : modelFallbacks) {
            if (currentModel == null || currentModel.isBlank()) {
                continue;
            }
            try {
                logger.info("Solicitando resposta do {} para pergunta ({} chars) usando modelo {}",
                        providerName, question.length(), currentModel);
                return callOpenAiCompatibleWithModel(question, apiKey, apiUrl, currentModel, providerName);
            } catch (RestClientException e) {
                lastError = e.getMessage();
                if (lastError != null && lastError.contains("model_not_found")) {
                    logger.warn("Modelo {} indisponível no {}, tentando próximo...", currentModel, providerName);
                    continue;
                }
                logger.error("Falha ao chamar {} com modelo {}: {}", providerName, currentModel, lastError);
                throw new RuntimeException("Serviço de IA (" + providerName + ") temporariamente indisponível: " + lastError, e);
            }
        }

        throw new RuntimeException("Não foi possível obter resposta do " + providerName
                + ". Último erro: " + (lastError != null ? lastError : "modelo indisponível"));
    }

    private String callOpenAiCompatibleWithModel(String question, String apiKey, String apiUrl,
                                                 String model, String providerName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        // Groq/OpenRouter ficam atrás de Cloudflare, que bloqueia (403/1010)
        // o User-Agent padrão do Java. Definimos um identificador próprio.
        headers.set("User-Agent", USER_AGENT);

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 1500);
        payload.put("top_p", 0.9);
        payload.put("stream", false);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", question)
        ));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException(providerName + " retornou status: " + response.getStatusCode());
        }

        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            String finishReason = (String) choice.get("finish_reason");
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            if (message != null && message.get("content") instanceof String answer
                    && !answer.isBlank()) {
                logger.info("Resposta obtida do {} usando modelo {} (finish_reason={})",
                        providerName, model, finishReason);
                return trimToSentenceBoundary(answer, "length".equals(finishReason));
            }
        }

        logger.error("Estrutura de resposta inválida do {}: {}", providerName, body);
        throw new RuntimeException("Estrutura de resposta inválida do " + providerName);
    }

    /**
     * Quando a API corta a resposta por limite de tokens ({@code finish_reason=length}),
     * recorta até a última frase completa para que o usuário nunca veja texto quebrado
     * no meio de uma palavra (ex.: "...que reduzam o").
     */
    private String trimToSentenceBoundary(String answer, boolean truncated) {
        String text = answer.trim();
        if (!truncated || text.isEmpty()) {
            return text;
        }
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                boolean endOfSentence = i == text.length() - 1
                        || text.charAt(i + 1) == ' '
                        || text.charAt(i + 1) == '\n';
                if (endOfSentence) {
                    return text.substring(0, i + 1).trim();
                }
            } else if (c == '\n') {
                return text.substring(0, i).trim();
            }
        }
        return text;
    }

    /**
     * Chama a Inference API do Hugging Face (formato diferente do OpenAI-compatible).
     */
    private String callHuggingFace(String question) {
        validateKey(huggingFaceKey, "Hugging Face");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + huggingFaceKey);
        headers.set("User-Agent", USER_AGENT);

        Map<String, String> payload = Map.of(
                "inputs", question,
                "parameters", "{\"max_new_tokens\": 500, \"temperature\": 0.7}"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
        String url = huggingFaceUrl.endsWith("/")
                ? huggingFaceUrl + huggingFaceModel
                : huggingFaceUrl + "/" + huggingFaceModel;

        ResponseEntity<List> response = restTemplate.postForEntity(url, request, List.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null
                || response.getBody().isEmpty()) {
            throw new RuntimeException("Hugging Face retornou status: " + response.getStatusCode());
        }

        Object first = response.getBody().get(0);
        if (first instanceof Map<?, ?> map && map.get("generated_text") instanceof String text
                && !text.isBlank()) {
            return text.trim();
        }

        throw new RuntimeException("Estrutura de resposta inválida do Hugging Face");
    }

    private void validateKey(String apiKey, String providerName) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("test-key-not-used")) {
            logger.error("Chave de API do {} não configurada.", providerName);
            throw new IllegalStateException("Serviço de IA não configurado. Configure a chave de API do " + providerName + ".");
        }
    }

    /**
     * Verifica disponibilidade do provedor ativo (health check).
     */
    public boolean isAvailable() {
        try {
            String apiKey;
            String modelsUrl;
            switch (getProviderName()) {
                case "openrouter" -> { apiKey = openRouterKey; modelsUrl = "https://openrouter.ai/api/v1/models"; }
                case "huggingface" -> { apiKey = huggingFaceKey; modelsUrl = huggingFaceUrl; }
                default -> { apiKey = groqKey; modelsUrl = "https://api.groq.com/openai/v1/models"; }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    modelsUrl, org.springframework.http.HttpMethod.GET, request, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Serviço de IA não disponível: {}", e.getMessage());
            return false;
        }
    }
}
