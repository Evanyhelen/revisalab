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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriquece as trilhas de estudo com conteúdo real de fontes públicas e gratuitas:
 *
 * <ul>
 *   <li><b>Wikipedia (REST)</b> — resumo teórico da área (título, descrição,
 *       extrato e link para o artigo completo). API sem chave.</li>
 *   <li><b>YouTube (oEmbed)</b> — metadados do vídeo quando há um ID curado;
 *       caso contrário, devolve um link de busca que leva o estudante direto
 *       para as aulas da disciplina.</li>
 * </ul>
 *
 * Ambas as chamadas usam timeout e {@code User-Agent} de navegador, no mesmo
 * padrão do {@link EnemService}. Falhas em qualquer fonte degradam graciosamente
 * para {@code null} (o front mostra apenas o que estiver disponível).
 */
@Service
public class StudyResourceService {

    private static final Logger logger = LoggerFactory.getLogger(StudyResourceService.class);

    private static final String WIKIPEDIA_SUMMARY_URL = "https://pt.wikipedia.org/api/rest_v1/page/summary/";
    private static final String YOUTUBE_OEMBED_URL = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=%s&format=json";
    private static final String YOUTUBE_SEARCH_URL = "https://www.youtube.com/results?search_query=";

    // Fontes públicas brasileiras (sem chave) para dados complementares por categoria.
    private static final String IBGE_STATES_URL = "https://servicodados.ibge.gov.br/api/v1/localidades/estados";
    private static final String BRASILAPI_HOLIDAYS_URL = "https://brasilapi.com.br/api/feriados/v1/%d";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";

    /** Termo canônico na Wikipédia para cada categoria de trilha. */
    private static final Map<String, String> WIKI_TERM = Map.ofEntries(
            Map.entry("Matemática", "Matemática"),
            Map.entry("Português", "Língua portuguesa"),
            Map.entry("Física", "Física"),
            Map.entry("Química", "Química"),
            Map.entry("Biologia", "Biologia"),
            Map.entry("História", "História do Brasil"),
            Map.entry("Geografia", "Geografia"),
            Map.entry("Raciocínio Lógico", "Lógica"),
            Map.entry("Redação", "Redação"),
            Map.entry("Filosofia", "Filosofia"),
            Map.entry("Sociologia", "Sociologia"),
            Map.entry("Inglês", "Língua inglesa")
    );

    /**
     * Termo canônico na Wikipédia para uma etapa específica (módulo) de uma
     * trilha, quando o título do módulo não corresponde a uma página real.
     * Etapas sem curadoria caem no {@link #WIKI_TERM} da categoria.
     */
    private static final Map<String, String> MODULE_WIKI_TERM = Map.ofEntries(
            Map.entry("Números e Operações", "Operação (matemática)"),
            Map.entry("Álgebra e Equações", "Equação"),
            Map.entry("Geometria e Medidas", "Geometria")
    );

    /**
     * Vídeos curados por categoria (ID do YouTube). Cada ID foi verificado via
     * oEmbed e aponta para uma aula introdutória real da disciplina. Quando não
     * há ID para a categoria, o serviço devolve um link de busca do YouTube, que
     * funciona para qualquer disciplina e nunca quebra por vídeo removido.
     *
     * Para adicionar uma aula oficial: insira aqui {@code "Categoria" -> "VIDEO_ID"}
     * e o front passará a embutir o player (oEmbed) automaticamente.
     */
    private static final Map<String, String> VIDEO_ID = Map.ofEntries(
            Map.entry("Matemática", "uaYFrtduFmI"),
            Map.entry("Português", "X61lboe5_pM"),
            Map.entry("Física", "9Ec63LG5nr0"),
            Map.entry("Química", "ZUFGXf9taEk"),
            Map.entry("Raciocínio Lógico", "KXm8Hi-I_G0"),
            Map.entry("História", "cuHddXfinDE"),
            Map.entry("Geografia", "tkn-YU7rleQ"),
            Map.entry("Biologia", "4FsLnuELY_E"),
            Map.entry("Redação", "CjUCTZstyK8"),
            Map.entry("Filosofia", "UK6OKSsastQ"),
            Map.entry("Sociologia", "u0C3F1Fq32E"),
            Map.entry("Inglês", "C-Mc6SlvrfM")
    );

    /**
     * Aulas específicas por título de trilha (conteúdo temático). Quando a trilha
     * tem um vídeo próprio, ele prevalece sobre o genérico da categoria; caso
     * contrário, cai no {@link #VIDEO_ID} por categoria e, por fim, na busca.
     */
    private static final Map<String, String> TITLE_VIDEO_ID = Map.ofEntries(
            Map.entry("Geometria e Trigonometria", "EieOYCmWHHM"),
            Map.entry("Literatura e Leitura", "jq3LBXYLZWY"),
            Map.entry("Óptica e Ondas", "QL-QjbzUJPw"),
            Map.entry("Química Orgânica", "XbPuaJjhSm0")
    );

    /**
     * Vídeos curados por etapa (módulo). Quando uma etapa não tem ID próprio,
     * o serviço devolve um link de busca do YouTube com o nome da etapa — assim
     * cada etapa do percurso apresenta conteúdo diferente, sem depender de vídeo
     * removido. Para embutir um player específico, adicione
     * {@code "Título da etapa" -> "VIDEO_ID"} aqui.
     */
    private static final Map<String, String> MODULE_VIDEO_ID = Map.ofEntries();

    private final RestTemplate restTemplate;

    public StudyResourceService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(15))
                .readTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Retorna {@code {article, video, extras}} para a categoria/título informados.
     * Nunca lança exceção — qualquer falha vira {@code null} naquela chave.
     */
    public Map<String, Object> getStudyResources(String category, String title) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("article", fetchWikipedia(category, title));
        result.put("video", fetchYoutube(category, title));
        result.put("extras", fetchExtras(category));
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchWikipedia(String category, String title) {
        // Etapa específica (módulo) quando houver curadoria; senão, o termo
        // canônico da categoria. Garante que trocar de etapa mude a teoria.
        String curated = (title != null) ? MODULE_WIKI_TERM.get(title) : null;
        String term = (curated != null && !curated.isBlank())
                ? curated
                : WIKI_TERM.getOrDefault(category, title);
        if (term == null || term.isBlank()) return null;

        URI url = UriComponentsBuilder
                .fromHttpUrl(WIKIPEDIA_SUMMARY_URL)
                .path(term)
                .build()
                .encode()
                .toUri();
        Map<String, Object> raw = exchange(url, Map.class);
        if (raw == null || raw.get("title") == null) return null;

        Map<String, Object> article = new LinkedHashMap<>();
        article.put("title", raw.get("title"));
        article.put("description", raw.get("description"));
        article.put("extract", raw.get("extract"));
        article.put("thumbnail", raw.get("thumbnail"));

        Object contentUrls = raw.get("content_urls");
        if (contentUrls instanceof Map<?, ?> cu && cu.get("desktop") instanceof Map<?, ?> desktop) {
            article.put("url", desktop.get("page"));
        } else {
            article.put("url", null);
        }
        return article;
    }

    private Map<String, Object> fetchYoutube(String category, String title) {
        // `title` é opcional (o front nem sempre envia) e as constantes acima são
        // Map.ofEntries(...), que não aceitam chave nula — um get(null) lançaria NPE
        // ("pk is null") e derrubaria todo o endpoint de recursos de estudo.
        String id = null;
        if (title != null) {
            id = TITLE_VIDEO_ID.get(title);      // vídeo curado por trilha
            if (id == null) {
                id = MODULE_VIDEO_ID.get(title); // vídeo curado por etapa
            }
        }
        if (id == null && (title == null || title.isBlank())) {
            id = VIDEO_ID.get(category);         // vídeo genérico só sem etapa específica
        }
        if (id != null && !id.isBlank()) {
            Map<String, Object> oembed = exchange(URI.create(String.format(YOUTUBE_OEMBED_URL, id)), Map.class);
            if (oembed != null && oembed.get("title") != null) {
                Map<String, Object> video = new LinkedHashMap<>();
                video.put("title", oembed.get("title"));
                video.put("author", oembed.get("author_name"));
                video.put("thumbnail", oembed.get("thumbnail_url"));
                video.put("watchUrl", "https://www.youtube.com/watch?v=" + id);
                video.put("embedUrl", "https://www.youtube.com/embed/" + id);
                return video;
            }
        }

        // Fallback confiável: busca no YouTube com o nome da etapa/disciplina + "aula".
        // Cada etapa gera uma busca diferente, então o conteúdo muda ao trocar de módulo.
        String topic = (title == null || title.isBlank()) ? category : title;
        String query = topic + " aula";
        Map<String, Object> video = new LinkedHashMap<>();
        video.put("title", topic);
        video.put("watchUrl", YOUTUBE_SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8));
        video.put("embedUrl", null);
        video.put("searchOnly", true);
        return video;
    }

    /** Dados complementares reais por categoria (IBGE para Geografia, feriados para História). */
    private Map<String, Object> fetchExtras(String category) {
        try {
            if ("Geografia".equals(category)) return fetchIbgeStates();
            if ("História".equals(category)) return fetchHolidays();
        } catch (Exception e) {
            logger.warn("Falha ao consultar dados complementares para {}: {}", category, e.getMessage());
        }
        return null;
    }

    /** Lista os 27 estados do Brasil (sigla + nome) agrupados por região, via IBGE. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchIbgeStates() {
        List<Map<String, Object>> states = exchange(URI.create(IBGE_STATES_URL), List.class);
        if (states == null || states.isEmpty()) return null;

        Map<String, List<String>> byRegion = new LinkedHashMap<>();
        for (Map<String, Object> s : states) {
            String regionName = "Brasil";
            if (s.get("regiao") instanceof Map<?, ?> r && r.get("nome") != null) {
                regionName = String.valueOf(r.get("nome"));
            }
            String sigla = s.get("sigla") != null ? String.valueOf(s.get("sigla")) : "";
            byRegion.computeIfAbsent(regionName, k -> new ArrayList<>()).add(sigla);
        }

        List<String> items = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : byRegion.entrySet()) {
            items.add(e.getKey() + " — " + String.join(", ", e.getValue()));
        }

        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("title", "Estados do Brasil por região");
        extras.put("items", items);
        return extras;
    }

    /** Lista os feriados nacionais do Brasil, via BrasilAPI (sem chave). */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchHolidays() {
        int year = LocalDate.now().getYear();
        List<Map<String, Object>> holidays = exchange(
                URI.create(String.format(BRASILAPI_HOLIDAYS_URL, year)), List.class);
        if (holidays == null || holidays.isEmpty()) return null;

        List<String> items = new ArrayList<>();
        for (Map<String, Object> h : holidays) {
            if (!"national".equals(String.valueOf(h.get("type")))) continue;
            String date = String.valueOf(h.get("date"));
            String dayMonth = date.length() >= 10
                    ? date.substring(8, 10) + "/" + date.substring(5, 7)
                    : date;
            items.add(dayMonth + " — " + h.get("name"));
        }

        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("title", "Feriados nacionais do Brasil (" + year + ")");
        extras.put("items", items);
        return extras;
    }

    private <T> T exchange(URI url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, request, responseType);
            return response.getBody();
        } catch (Exception e) {
            logger.warn("Falha ao consultar fonte de estudo ({}): {}", url, e.getMessage());
            return null;
        }
    }
}
