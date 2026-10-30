package com.revisalab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Integração com a API pública de questões do ENEM (https://enem.dev).
 *
 * A API é gratuita, sem chave, limitada a 1 requisição/segundo. Como o conteúdo
 * completo de cada questão exige uma chamada individual (a listagem retorna só
 * os metadados), as respostas são cacheadas em memória para não re-buscar a
 * mesma questão.
 *
 * Disciplinas da API: {@code matematica}, {@code ciencias-natureza},
 * {@code ciencias-humanas} e {@code linguagens}.
 */
@Service
public class EnemService {

    private static final Logger logger = LoggerFactory.getLogger(EnemService.class);

    private static final String BASE_URL = "https://api.enem.dev/v1";
    private static final int DEFAULT_YEAR = 2023;
    private static final long REQUEST_INTERVAL_MS = 1050; // respeita o limite de 1 req/s

    // A API fica atrás de um proxy/CDN que exige um User-Agent de navegador.
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    private static final Map<String, String> DISCIPLINE_LABELS = Map.of(
            "matematica", "Matemática (ENEM)",
            "ciencias-natureza", "Ciências da Natureza (ENEM)",
            "ciencias-humanas", "Ciências Humanas (ENEM)",
            "linguagens", "Linguagens (ENEM)"
    );

    private final RestTemplate restTemplate;
    private final Map<String, Object> questionCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<Map<String, Object>>> examCache = new ConcurrentHashMap<>();
    private volatile List<Integer> yearsCache;

    // Controla o intervalo entre requisições à API do ENEM (limite de 1 req/s).
    // Serializa as chamadas HTTP e registra o instante de início da última.
    private final Object rateLimitLock = new Object();
    private long lastRequestAt = 0;

    public EnemService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(20))
                .readTimeout(Duration.ofSeconds(20))
                .build();
    }

    /**
     * Retorna questões normalizadas de uma disciplina do ENEM.
     */
    public List<Map<String, Object>> getQuestions(String discipline, Integer year, int limit) {
        int examYear = year != null ? year : pickRandomYear();
        String normalized = discipline == null ? "matematica" : discipline.trim().toLowerCase();

        List<Map<String, Object>> metadata = getExamMetadata(examYear).stream()
                .filter(q -> normalized.equals(q.get("discipline")))
                .filter(q -> q.get("language") == null) // ignora variantes de idioma estrangeiro
                .collect(Collectors.toList());

        List<Map<String, Object>> result = collectQuestions(examYear, metadata, limit);
        if (result.isEmpty() && !metadata.isEmpty()) {
            logger.warn("Nenhuma questão ENEM textual válida para {} em {}", normalized, examYear);
        }
        return result;
    }

    /**
     * Retorna questões ENEM válidas misturando todas as disciplinas (para o
     * Construtor de Base), sorteando um ano a cada chamada para variar entre sessões.
     */
    public List<Map<String, Object>> getMixedQuestions(int limit) {
        int examYear = pickRandomYear();
        List<Map<String, Object>> metadata = getExamMetadata(examYear).stream()
                .filter(q -> DISCIPLINE_LABELS.containsKey(q.get("discipline")))
                .filter(q -> q.get("language") == null)
                .collect(Collectors.toList());
        return collectQuestions(examYear, metadata, limit);
    }

    /**
     * Resolve o gabarito de uma questão ENEM a partir do id determinístico exposto
     * ao cliente ({@code year*1000 + index}). Usado pelo Construtor de Base para
     * validar a resposta no servidor, sem precisar enviar o gabarito na listagem
     * de questões. Retorna null quando a questão não existe mais ou é inválida.
     */
    public Map<String, String> resolveCorrectAnswer(long questionId) {
        int year = (int) (questionId / 1000L);
        int index = (int) (questionId % 1000L);
        Map<String, Object> question = getQuestion(year, index);
        if (!isValid(question)) {
            return null;
        }
        String correct = (String) question.get("correctAnswer");
        String explanation = (String) question.get("explanation");
        Map<String, String> result = new LinkedHashMap<>();
        result.put("correctAnswer", correct);
        result.put("explanation", explanation == null ? "" : explanation);
        return result;
    }

    /**
     * Sorteia os enunciados da prova e coleta até {@code limit} questões válidas
     * (com alternativas textuais e gabarito), substituindo as inválidas.
     */
    private List<Map<String, Object>> collectQuestions(int year, List<Map<String, Object>> metadata, int limit) {
        Collections.shuffle(metadata);

        List<Map<String, Object>> result = new ArrayList<>();
        int cursor = 0;
        int attempts = 0;
        int maxAttempts = metadata.size() + 20; // margem de segurança
        while (result.size() < limit && cursor < metadata.size() && attempts < maxAttempts) {
            attempts++;
            int index = ((Number) metadata.get(cursor).get("index")).intValue();
            cursor++;

            Map<String, Object> question = getQuestion(year, index);
            if (isValid(question)) {
                result.add(question);
            } else {
                // Substitui questões cujas alternativas são figuras (sem texto) por outras válidas.
                logger.info("Substituindo questão ENEM inválida (sem alternativas textuais): {} Q{}", year, index);
            }
            // O intervalo entre requisições é aplicado em exchange(), que controla
            // o limite de 1 req/s (inclusive para requisições em cache miss).
        }
        return result;
    }

    /**
     * Retorna os anos disponíveis na API.
     */
    @SuppressWarnings("unchecked")
    public List<Integer> getYears() {
        if (yearsCache != null) return yearsCache;
        List<Map<String, Object>> exams = exchange(BASE_URL + "/exams", List.class);
        if (exams == null) return List.of(DEFAULT_YEAR);
        yearsCache = exams.stream()
                .map(e -> ((Number) e.get("year")).intValue())
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        return yearsCache;
    }

    /** Sorteia um ano de prova (variedade entre sessões) quando o cliente não especifica. */
    private int pickRandomYear() {
        List<Integer> years = getYears();
        if (years == null || years.isEmpty()) return DEFAULT_YEAR;
        return years.get(new Random().nextInt(years.size()));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getExamMetadata(int year) {
        return examCache.computeIfAbsent(year, y -> {
            Map<String, Object> exam = exchange(BASE_URL + "/exams/" + y, Map.class);
            if (exam != null && exam.get("questions") instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
            return List.of();
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getQuestion(int year, int index) {
        String key = year + "-" + index;
        return (Map<String, Object>) questionCache.computeIfAbsent(key, k -> {
            Map<String, Object> raw = exchange(BASE_URL + "/exams/" + year + "/questions/" + index, Map.class);
            if (raw == null) {
                // Falha na API: retorna sentinela inválida que será filtrada por isValid().
                return Map.of("question", "", "options", List.of());
            }
            return normalize(raw, year, index);
        });
    }

    private Map<String, Object> normalize(Map<String, Object> raw, int year, int index) {
        String context = clean((String) raw.get("context"));
        String intro = clean((String) raw.get("alternativesIntroduction"));
        String questionText = (context + (intro != null && !intro.isBlank() ? "\n\n" + intro : "")).trim();

        String correct = raw.get("correctAlternative") != null ? raw.get("correctAlternative").toString() : "";

        List<Map<String, String>> options = new ArrayList<>();
        Object alts = raw.get("alternatives");
        if (alts instanceof List<?> list) {
            for (Object alt : list) {
                if (alt instanceof Map<?, ?> m) {
                    String letter = m.get("letter") != null ? String.valueOf(m.get("letter")) : "";
                    String text = m.get("text") != null ? clean(String.valueOf(m.get("text"))) : "";
                    // Descarta alternativas sem texto (questões cujas alternativas são figuras).
                    if (!text.isBlank()) {
                        options.add(Map.of("id", letter, "text", text));
                    }
                }
            }
        }

        String discipline = raw.get("discipline") != null ? raw.get("discipline").toString() : "";
        String category = DISCIPLINE_LABELS.getOrDefault(discipline, "ENEM");

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", (long) year * 1000L + index);
        dto.put("question", questionText);
        dto.put("type", "multiple-choice");
        dto.put("options", options);
        dto.put("correctAnswer", correct);
        dto.put("explanation", "Questão " + index + " do ENEM " + year + ". Gabarito oficial: " + correct + ".");
        dto.put("difficulty", 2);
        dto.put("category", category);
        dto.put("xpReward", 10);
        dto.put("source", "enem");
        dto.put("year", year);
        dto.put("index", index);
        return dto;
    }

    /**
     * Valida se a questão é exibível como múltipla escolha textual:
     * precisa de enunciado, de alternativas com texto (não figuras) e de gabarito.
     */
    private boolean isValid(Map<String, Object> question) {
        String q = (String) question.get("question");
        if (q == null || q.isBlank()) return false;

        Object opts = question.get("options");
        if (!(opts instanceof List<?> list) || list.size() < 2) return false;

        String correct = (String) question.get("correctAnswer");
        if (correct == null || correct.isBlank()) return false;

        boolean hasCorrectOption = false;
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                Object t = m.get("text");
                if (t == null || String.valueOf(t).isBlank()) return false;
                if (correct.equals(m.get("id"))) hasCorrectOption = true;
            } else {
                return false;
            }
        }
        // O gabarito precisa corresponder a uma alternativa textual que foi preservada.
        return hasCorrectOption;
    }

    /** Remove markdown (imagens/bold) e colapsa espaços, para exibição limpa. */
    private String clean(String value) {
        if (value == null) return "";
        return value
                .replaceAll("!\\[[^\\]]*]\\([^)]*\\)", "") // imagens markdown
                .replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1") // bold/italic
                .replaceAll("[ \t]+", " ")
                .trim();
    }

    private <T> T exchange(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            // Respeita o limite de 1 req/s da API. Aguarda apenas o tempo restante
            // desde o INÍCIO da última requisição (a latência da própria chamada
            // já conta como parte do intervalo), em vez de somar um sleep fixo após
            // cada resposta — isso evita dobrar a espera em chamadas lentas.
            synchronized (rateLimitLock) {
                long now = System.currentTimeMillis();
                long wait = lastRequestAt + REQUEST_INTERVAL_MS - now;
                if (wait > 0) sleep(wait);
                lastRequestAt = System.currentTimeMillis();
                ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, request, responseType);
                return response.getBody();
            }
        } catch (Exception e) {
            logger.warn("Falha ao consultar API ENEM ({}): {}", url, e.getMessage());
            return null;
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
