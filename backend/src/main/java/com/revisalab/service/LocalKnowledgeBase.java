package com.revisalab.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class LocalKnowledgeBase {

    private final Map<String, String> conceptDictionary = new LinkedHashMap<>();
    private final Map<String, List<String>> studyTips = new LinkedHashMap<>();
    private final List<String> availableSubjects = new ArrayList<>();

    public LocalKnowledgeBase() {
        initializeDictionary();
        initializeStudyTips();
        initializeSubjects();
    }

    public String getAnswer(String question, String context) {
        if (question == null || question.isBlank()) {
            return "Olá! 👋 Sou o assistente do RevisaLab. Pode me fazer perguntas sobre Matemática, Português, Física, Química, Biologia, História, Geografia ou métodos de estudo. Como posso te ajudar?";
        }

        // Normaliza acentos (permite "ciencias" casar com "ciências", "equacao" com "equação", etc.)
        String q = normalize(question).toLowerCase().trim();

        // Saudações — casamento por palavra inteira, evitando "foi"→"oi", "feito"→"ei", "Europa"→"opa".
        if (q.matches(".*\\b(ola|oi|oie|ei|hey|opa|tudo bem|tudo bom|como vai|bom dia|boa tarde|boa noite)\\b.*")) {
            return "Olá! 👋 Sou o assistente de estudos do RevisaLab. Estou aqui para te ajudar com suas dúvidas de conteúdo.\n\nPosso explicar conceitos de **Matemática, Português, Física, Química, Biologia, História e Geografia**.\n\nQual assunto quer estudar hoje?";
        }

        // Agradecimentos
        if (q.matches(".*\\b(obrigad\\w*|valeu|vlw|thanks|brigad\\w*)\\b.*")) {
            return "De nada! 😊 Sempre que tiver dúvidas, pode perguntar. Bons estudos!";
        }

        // Raiz quadrada numérica: "raiz quadrada de 25", "raiz de 9".
        java.util.regex.Matcher sqrtMatcher = Pattern.compile("raiz(?:\\s+quadrada)?\\s+de\\s+(\\d+(?:[.,]\\d+)?)").matcher(q);
        if (sqrtMatcher.find()) {
            String sqrtAnswer = squareRootAnswer(sqrtMatcher.group(1));
            if (sqrtAnswer != null) {
                return sqrtAnswer;
            }
        }

        // Busca no dicionário — varredura completa (palavras-chave múltiplas por entrada),
        // com normalização de acentos e casamento por palavra inteira (sufixo plural opcional),
        // evitando falsos positivos como "buracos"→"cos" ou "revolução"→"evolução".
        for (Map.Entry<String, String> entry : conceptDictionary.entrySet()) {
            String[] keys = entry.getKey().split("\\|");
            for (String key : keys) {
                String k = normalize(key).toLowerCase().trim();
                if (k.isEmpty()) continue;

                if (q.matches(".*\\b" + Pattern.quote(k) + "(s|es)?\\b.*")) {
                    return entry.getValue();
                }
            }
        }

        return generateGenericAnswer(question, context);
    }

    /** Remove acentos para tornar o casamento insensível a "ção/ção", "é/e", "á/a", etc. */
    private static String normalize(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private String squareRootAnswer(String numberStr) {
        try {
            double n = Double.parseDouble(numberStr.replace(',', '.'));
            if (n < 0) {
                return "📐 **Raiz quadrada de " + numberStr + "**\n\nNão existe raiz quadrada real de um número negativo (ela pertence ao conjunto dos números complexos).";
            }
            double r = Math.sqrt(n);
            if (r == Math.floor(r)) {
                long whole = (long) r;
                String nStr = (n == Math.floor(n)) ? String.valueOf((long) n) : numberStr;
                return "📐 **Raiz quadrada de " + nStr + "**\n\n√" + nStr + " = **" + whole + "**, porque " + whole + " × " + whole + " = " + nStr + ".\n\n💡 *A raiz quadrada é o número que, multiplicado por si mesmo, resulta no valor dado.*";
            }
            return "📐 **Raiz quadrada de " + numberStr + "**\n\n√" + numberStr + " ≈ **" + String.format(java.util.Locale.US, "%.2f", r) + "** (valor aproximado).";
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String[] getAvailableSubjects() {
        return availableSubjects.toArray(new String[0]);
    }

    public List<String> getStudyTipsForSubject(String subject) {
        return studyTips.getOrDefault(subject.toLowerCase(), getGenericTips());
    }

    private void initializeDictionary() {

        // ── MATEMÁTICA ────────────────────────────────────────────────────────────
        conceptDictionary.put("derivada|derivar|taxa de mudança",
            "📐 **Derivada**\n\nDerivada é a taxa de variação instantânea de uma função. Geometricamente, é a inclinação da reta tangente à curva em um ponto.\n\n**Regras básicas:**\n• Constante: d/dx(c) = 0\n• Potência: d/dx(xⁿ) = n·xⁿ⁻¹\n• Soma: d/dx(f+g) = f' + g'\n\n**Exemplo:** A derivada de f(x) = x² é f'(x) = 2x.\n\n💡 *Dica: pratique as regras básicas antes de partir para a regra da cadeia.*");

        conceptDictionary.put("integral|integrar|primitiva|antiderivada",
            "📐 **Integral**\n\nA integral é a operação inversa da derivada. Representa a área sob a curva de uma função.\n\n**Tipos:**\n• **Indefinida:** ∫f(x)dx = F(x) + C (onde C é constante)\n• **Definida:** ∫ₐᵇf(x)dx = F(b) − F(a)\n\n**Exemplo:** ∫x² dx = x³/3 + C\n\n💡 *Dica: domine as derivadas antes de estudar integrais.*");

        conceptDictionary.put("equação|equações|incógnita",
            "📐 **Equação**\n\nEquação é uma igualdade com uma ou mais incógnitas. Resolver é encontrar o valor que torna a igualdade verdadeira.\n\n**Tipos:**\n• 1º grau: ax + b = 0 → x = −b/a\n• 2º grau: ax² + bx + c = 0 → Bhaskara: x = (−b ± √Δ) / 2a, onde Δ = b²−4ac\n\n**Exemplo:** 2x + 6 = 14 → 2x = 8 → x = 4");

        conceptDictionary.put("bhaskara|fórmula quadrática|discriminante",
            "📐 **Fórmula de Bhaskara**\n\nUsada para resolver equações do 2º grau: **ax² + bx + c = 0**\n\n**Δ = b² − 4ac** (discriminante)\n\n**x = (−b ± √Δ) / 2a**\n\n• Δ > 0 → duas raízes reais distintas\n• Δ = 0 → uma raiz real (dupla)\n• Δ < 0 → sem raízes reais\n\n**Exemplo:** x² − 5x + 6 = 0 → Δ = 25−24 = 1 → x = (5±1)/2 → x₁=3, x₂=2");

        conceptDictionary.put("função|funções|domínio|imagem|contradomínio",
            "📐 **Função**\n\nFunção f: A → B relaciona cada elemento de A a exatamente um elemento de B.\n\n**Tipos comuns:**\n• **Linear:** f(x) = ax + b (reta)\n• **Quadrática:** f(x) = ax² + bx + c (parábola)\n• **Exponencial:** f(x) = aˣ\n• **Logarítmica:** f(x) = log_a(x)\n\n**Termos-chave:** Domínio = conjunto A; Imagem = valores assumidos por f; Contradomínio = conjunto B.");

        conceptDictionary.put("logaritmo|log|ln",
            "📐 **Logaritmo**\n\nlogₐ(b) = x significa aˣ = b. O logaritmo é o expoente ao qual a base deve ser elevada para obter o número.\n\n**Propriedades:**\n• logₐ(M·N) = logₐM + logₐN\n• logₐ(M/N) = logₐM − logₐN\n• logₐ(Mⁿ) = n·logₐM\n\n**Exemplo:** log₂(64) = 6 pois 2⁶ = 64\n\n💡 *log natural ln = logₑ (base e ≈ 2,718)*");

        conceptDictionary.put("trigonometria|seno|cosseno|tangente|sen|cos|tg",
            "📐 **Trigonometria**\n\nNo triângulo retângulo de ângulo θ:\n• **sen(θ) = cateto oposto / hipotenusa**\n• **cos(θ) = cateto adjacente / hipotenusa**\n• **tg(θ) = sen(θ) / cos(θ)**\n\n**Ângulos especiais:**\n| θ | sen | cos | tg |\n|---|-----|-----|----|\n| 30° | 1/2 | √3/2 | √3/3 |\n| 45° | √2/2 | √2/2 | 1 |\n| 60° | √3/2 | 1/2 | √3 |");

        conceptDictionary.put("progressão aritmética|pa|razão|termo geral",
            "📐 **Progressão Aritmética (PA)**\n\nSequência onde a diferença entre termos consecutivos é constante (razão r).\n\n• **Termo geral:** aₙ = a₁ + (n−1)·r\n• **Soma:** Sₙ = n·(a₁ + aₙ) / 2\n\n**Exemplo:** 2, 5, 8, 11, ... → r = 3; a₅ = 2 + 4·3 = 14");

        conceptDictionary.put("progressão geométrica|pg|razão geométrica",
            "📐 **Progressão Geométrica (PG)**\n\nSequência onde o quociente entre termos consecutivos é constante (razão q).\n\n• **Termo geral:** aₙ = a₁ · qⁿ⁻¹\n• **Soma finita:** Sₙ = a₁ · (qⁿ − 1) / (q − 1)\n\n**Exemplo:** 3, 6, 12, 24, ... → q = 2; a₅ = 3·2⁴ = 48");

        conceptDictionary.put("probabilidade|evento|espaço amostral",
            "📐 **Probabilidade**\n\nP(A) = número de casos favoráveis / total de casos possíveis\n\n**Propriedades:**\n• 0 ≤ P(A) ≤ 1\n• P(A) + P(Ā) = 1\n• P(A ∪ B) = P(A) + P(B) − P(A ∩ B)\n\n**Exemplo:** Lançar um dado: P(par) = 3/6 = 1/2");

        conceptDictionary.put("geometria|área|perímetro|volume",
            "📐 **Geometria — Fórmulas essenciais**\n\n**Plana:**\n• Quadrado: A = l² ; P = 4l\n• Retângulo: A = b·h ; P = 2(b+h)\n• Triângulo: A = base·altura/2\n• Círculo: A = πr² ; C = 2πr\n\n**Espacial:**\n• Cubo: V = a³\n• Esfera: V = 4πr³/3\n• Cilindro: V = πr²·h");

        conceptDictionary.put("raiz quadrada|radiciação|raiz cúbica|potenciação|potência",
            "📐 **Raiz Quadrada / Radiciação**\n\nA raiz quadrada de um número é o valor que, multiplicado por si mesmo, resulta nesse número.\n\n• √25 = 5, porque 5 × 5 = 25\n• √64 = 8, porque 8 × 8 = 64\n• √144 = 12\n• √9 = 3\n\n**Propriedades:**\n• √(a·b) = √a · √b\n• √(a/b) = √a / √b\n\n**Potenciação relacionada:** a raiz é a operação inversa da potência: 5² = 25 ⇔ √25 = 5.\n\n💡 *Para calcular a raiz de um número específico, pergunte: 'raiz quadrada de 36'.*");

        // ── PORTUGUÊS ────────────────────────────────────────────────────────────
        conceptDictionary.put("substantivo|nome",
            "📖 **Substantivo**\n\nClasse de palavra que nomeia seres, lugares, objetos, sentimentos, ações ou qualidades.\n\n**Classificações:**\n• **Próprio** (escrito com maiúscula): Brasil, Maria\n• **Comum:** cidade, cachorro\n• **Concreto:** livro, mesa\n• **Abstrato:** amor, liberdade\n• **Coletivo:** enxame (abelhas), alcateia (lobos)\n\n**Flexões:** gênero (masculino/feminino), número (singular/plural) e grau.");

        conceptDictionary.put("verbo|conjugar|conjugação|tempo verbal",
            "📖 **Verbo**\n\nPalavra que indica ação, estado ou fenômeno. Flexiona em pessoa, número, tempo e modo.\n\n**Modos verbais:**\n• **Indicativo:** fato real (Eu estudo)\n• **Subjuntivo:** hipótese/desejo (Se eu estudasse)\n• **Imperativo:** ordem/pedido (Estude!)\n\n**Tempos:** presente, pretérito (perfeito/imperfeito/mais-que-perfeito), futuro.");

        conceptDictionary.put("sujeito|predicado|oração",
            "📖 **Análise Sintática**\n\n• **Sujeito:** ser sobre o qual se faz uma declaração. Concorda com o verbo em número e pessoa.\n• **Predicado:** o que se declara sobre o sujeito; contém verbo.\n• **Objeto direto:** complemento verbal sem preposição.\n• **Objeto indireto:** complemento verbal com preposição.\n\n**Exemplo:** *'O aluno estudou matemática'* — Sujeito: O aluno; Predicado: estudou matemática; OD: matemática.");

        conceptDictionary.put("concordância verbal|concordância nominal",
            "📖 **Concordância**\n\n**Verbal:** o verbo concorda com o sujeito em número e pessoa.\n• 'Os alunos **estudaram**' ✓\n• 'Os alunos **estudou**' ✗\n\n**Nominal:** adjetivo, artigo, numeral e pronome concordam com o substantivo.\n• 'As alunas **dedicadas**' ✓\n\n**Casos especiais:** sujeito coletivo (verbo no singular), 'mais de um' (singular ou plural).");

        conceptDictionary.put("crase|acento grave",
            "📖 **Crase**\n\nCrase é a fusão da preposição 'a' com o artigo definido feminino 'a'. Indica-se com acento grave (à).\n\n**Regra prática:** substitua por 'para a' — se ficar certo, há crase.\n\n**Usa-se:** 'Fui **à** escola' (fui para a escola ✓)\n**Não usa-se:** 'Fui a pé' (sem artigo); 'Avisei a ele' (pronome);\n\n**Dica:** nunca há crase antes de verbo ou palavra masculina (exceto 'moda de').");

        conceptDictionary.put("figura de linguagem|metáfora|hipérbole|ironia|metonímia|antítese",
            "📖 **Figuras de Linguagem**\n\n• **Metáfora:** comparação implícita — 'Ele é uma pedra'\n• **Comparação/Símile:** comparação explícita — 'Ele é duro como pedra'\n• **Hipérbole:** exagero — 'Estou morrendo de fome'\n• **Ironia:** dizer o contrário — 'Que dia lindo!' (sob chuva)\n• **Metonímia:** substituição de um termo por outro relacionado — 'Ler Machado de Assis'\n• **Antítese:** ideias opostas — 'Amor e ódio'\n• **Eufemismo:** suavizar — 'Ele partiu' (morreu)");

        conceptDictionary.put("redação|dissertação|argumentação|texto dissertativo",
            "📖 **Redação Dissertativa-Argumentativa**\n\n**Estrutura:**\n1. **Introdução** (apresentar tese) — 5-7 linhas\n2. **Desenvolvimento** (2 parágrafos, 1 argumento cada) — 8-10 linhas cada\n3. **Conclusão** (retomar tese + proposta de intervenção) — 5-7 linhas\n\n**Para o ENEM:** a proposta de intervenção deve ter agente, ação, modo/meio, finalidade e detalhamento.\n\n💡 *Dica: cada parágrafo de desenvolvimento = tópico frasal + desenvolvimento + conclusão parcial.*");

        conceptDictionary.put("pronome|pronomes pessoais|pronomes relativos",
            "📖 **Pronomes**\n\nSubstituem ou acompanham substantivos.\n\n**Pessoais:** eu, tu, ele/ela, nós, vós, eles/elas (retos); me, te, se, nos, vos (oblíquos)\n**Possessivos:** meu, teu, seu, nosso, vosso\n**Demonstrativos:** este/esse/aquele (e variações)\n**Relativos:** que, quem, cujo, onde, quanto\n\n💡 *'Cujo' = de quem; nunca se usa 'cujo' com artigo depois: 'cujo o' está errado.*");

        // ── FÍSICA ────────────────────────────────────────────────────────────────
        conceptDictionary.put("velocidade|velocidade média|velocidade escalar",
            "🔭 **Velocidade**\n\n**Velocidade média:** v = Δs / Δt\n\n• Δs = variação de posição (m)\n• Δt = variação de tempo (s)\n• Unidade: m/s (ou km/h)\n\n**Conversão:** 1 m/s = 3,6 km/h\n\n**Exemplo:** Um carro percorre 180 km em 2 h → v = 180/2 = **90 km/h**\n\n**MRU (Movimento Retilíneo Uniforme):** v = constante, a = 0");

        conceptDictionary.put("aceleração|mruv|movimento uniformemente variado",
            "🔭 **Aceleração / MRUV**\n\n**Aceleração:** a = Δv / Δt (m/s²)\n\n**Equações do MRUV:**\n• v = v₀ + a·t\n• s = s₀ + v₀·t + ½·a·t²\n• v² = v₀² + 2a·Δs (Torricelli)\n\n**Exemplo:** Carro parte do repouso com a = 4 m/s²; após 3s: v = 4·3 = 12 m/s; s = ½·4·9 = 18 m");

        conceptDictionary.put("força|newton|segunda lei|leis de newton",
            "🔭 **Leis de Newton**\n\n**1ª Lei (Inércia):** Um corpo em repouso permanece em repouso e em movimento permanece em movimento a menos que uma força aja sobre ele.\n\n**2ª Lei:** **F = m·a**\n• F em Newtons (N), m em kg, a em m/s²\n\n**3ª Lei (Ação e Reação):** Para toda ação há uma reação igual e oposta.\n\n**Exemplo:** Empurrar uma caixa de 10 kg com F = 50 N → a = F/m = 50/10 = **5 m/s²**");

        conceptDictionary.put("energia|energia cinética|energia potencial|trabalho|potência",
            "🔭 **Energia e Trabalho**\n\n• **Trabalho:** W = F·d·cos(θ) (Joules)\n• **Energia cinética:** Ec = ½·m·v²\n• **Energia potencial gravitacional:** Ep = m·g·h\n• **Potência:** P = W/t (Watts)\n• **Conservação de energia:** Ec + Ep = constante (sem atrito)\n\n**Exemplo:** Objeto de 2 kg a 10 m/s → Ec = ½·2·100 = **100 J**");

        conceptDictionary.put("termodinâmica|temperatura|calor|entropia|lei da termodinâmica",
            "🔭 **Termodinâmica**\n\n**Leis:**\n• **0ª Lei:** Se A está em equilíbrio com B e B com C, então A está com C.\n• **1ª Lei:** ΔU = Q − W (energia interna = calor absorvido − trabalho realizado)\n• **2ª Lei:** O calor não flui espontaneamente do frio para o quente (entropia aumenta).\n\n**Escalas de temperatura:**\nT(K) = T(°C) + 273; T(°F) = T(°C)·9/5 + 32");

        conceptDictionary.put("eletricidade|elétrica|corrente elétrica|tensão|resistência|ohm",
            "🔭 **Eletricidade**\n\n**Lei de Ohm:** V = R·I\n• V = tensão (Volts), R = resistência (Ohms), I = corrente (Amperes)\n\n**Potência elétrica:** P = V·I = R·I² = V²/R\n\n**Associações:**\n• **Série:** R_total = R₁ + R₂ + ...\n• **Paralelo:** 1/R_total = 1/R₁ + 1/R₂ + ...\n\n**Exemplo:** V = 12V, R = 3Ω → I = 12/3 = **4 A**");

        conceptDictionary.put("óptica|luz|reflexão|refração|espelho|lente",
            "🔭 **Óptica**\n\n**Reflexão:** ângulo de incidência = ângulo de reflexão\n\n**Refração (Snell-Descartes):** n₁·sen(θ₁) = n₂·sen(θ₂)\n\n**Lentes (Gauss):** 1/f = 1/p + 1/p'\n• f > 0: lente convergente\n• f < 0: lente divergente\n\n**Espelhos côncavos:** formam imagens reais e invertidas (objeto além do foco).");

        // ── QUÍMICA ────────────────────────────────────────────────────────────────
        conceptDictionary.put("átomo|próton|nêutron|elétron|núcleo",
            "⚗️ **Átomo**\n\nUnidade básica da matéria:\n• **Próton** (p⁺): carga positiva, no núcleo\n• **Nêutron** (n⁰): sem carga, no núcleo\n• **Elétron** (e⁻): carga negativa, na eletrosfera\n\n**Número atômico (Z)** = número de prótons\n**Número de massa (A)** = prótons + nêutrons\n\n**Exemplo:** Carbono ¹²₆C → Z=6, A=12, nêutrons=6");

        conceptDictionary.put("tabela periódica|elemento|período|grupo|família",
            "⚗️ **Tabela Periódica**\n\n**Organização:** elementos ordenados por número atômico crescente.\n\n**Grupos (colunas):**\n• Família IA (1): metais alcalinos\n• Família IIA (2): metais alcalino-terrosos\n• Família VIIA (17): halogênios\n• Família 0 (18): gases nobres\n\n**Períodos (linhas):** indicam o nível de energia mais externo.\n\n**Propriedades periódicas:** raio atômico, eletronegatividade, energia de ionização.");

        conceptDictionary.put("ligação química|ligação iônica|ligação covalente|ligação metálica",
            "⚗️ **Ligações Químicas**\n\n• **Iônica:** transferência de e⁻ (metal + não-metal) → Ex: NaCl\n• **Covalente:** compartilhamento de e⁻ (não-metal + não-metal) → Ex: H₂O, CO₂\n• **Metálica:** 'mar de elétrons' (metais) → conduz eletricidade\n\n**Polaridade:**\n• Ligação apolar: Δeletronegatividade ≈ 0\n• Ligação polar: Δeletronegatividade > 0,4");

        conceptDictionary.put("reação química|reagente|produto|balancear|equação química",
            "⚗️ **Reações Químicas**\n\n**Tipos:**\n• **Síntese:** A + B → AB\n• **Decomposição/Análise:** AB → A + B\n• **Deslocamento:** A + BC → AC + B\n• **Dupla troca:** AB + CD → AD + CB\n\n**Balanceamento:** conservação de massa — número de átomos de cada elemento igual nos dois lados.\n\nEx: H₂ + O₂ → H₂O (não balanceada) → 2H₂ + O₂ → 2H₂O ✓");

        conceptDictionary.put("mol|molar|avogadro|massa molar",
            "⚗️ **Mol e Quantidade de Matéria**\n\n**1 mol** = 6,02 × 10²³ partículas (Número de Avogadro)\n\n**Massa molar (M):** massa de 1 mol em g/mol (= massa atômica em g/mol)\n\n**Relações:**\n• n = m / M (mol = massa / massa molar)\n• n = N / Nₐ (mol = número de partículas / Avogadro)\n• n = V / 22,4 (mol = volume a 0°C, 1 atm)\n\nEx: 36g de H₂O (M=18) → n = 36/18 = **2 mol**");

        conceptDictionary.put("ph|potencial hidrogeniônico|ácido|base|neutralização",
            "⚗️ **Ácido-Base e pH**\n\n**pH = −log[H⁺]**\n• pH < 7: ácido\n• pH = 7: neutro\n• pH > 7: básico/alcalino\n\n**Teoria de Arrhenius:**\n• Ácido: libera H⁺ em água → Ex: HCl → H⁺ + Cl⁻\n• Base: libera OH⁻ em água → Ex: NaOH → Na⁺ + OH⁻\n\n**Neutralização:** ácido + base → sal + água\n• HCl + NaOH → NaCl + H₂O");

        conceptDictionary.put("orgânica|hidrocarboneto|alcano|alceno|alquino|benzeno",
            "⚗️ **Química Orgânica**\n\n**Hidrocarbonetos:**\n• **Alcanos** (CₙH₂ₙ₊₂): ligações simples — metano, etano\n• **Alcenos** (CₙH₂ₙ): 1 ligação dupla — eteno, propeno\n• **Alquinos** (CₙH₂ₙ₋₂): 1 ligação tripla — acetileno\n• **Aromáticos:** anel benzênico — benzeno, tolueno\n\n**Grupos funcionais importantes:** álcool (–OH), ácido carboxílico (–COOH), éster (–COO–), cetona (–CO–)");

        // ── BIOLOGIA ────────────────────────────────────────────────────────────────
        conceptDictionary.put("célula|membrana|núcleo|citoplasma|organela",
            "🧬 **Célula**\n\nUnidade fundamental da vida.\n\n**Procariontes:** sem núcleo delimitado (bactérias, archaea)\n**Eucariontes:** com núcleo definido por membrana (animais, plantas, fungos)\n\n**Organelas principais:**\n• **Mitocôndria:** respiração celular (ATP)\n• **Cloroplasto:** fotossíntese (plantas)\n• **Ribossomo:** síntese de proteínas\n• **Retículo endoplasmático:** transporte e processamento\n• **Núcleo:** armazena DNA");

        conceptDictionary.put("fotossíntese|clorofila|glicose|luz solar|cloroplasto",
            "🧬 **Fotossíntese**\n\nProcesso pelo qual plantas, algas e cianobactérias produzem glicose a partir de luz solar.\n\n**Equação geral:**\n6CO₂ + 6H₂O + luz → C₆H₁₂O₆ + 6O₂\n\n**Fases:**\n1. **Fase fotoquímica (clara):** ocorre na membrana dos tilacoides; captura luz, fotólise da água, produção de ATP e NADPH\n2. **Fase química (escura/ciclo de Calvin):** no estroma; fixação de CO₂ e produção de glicose");

        conceptDictionary.put("respiração celular|mitocôndria|atp|aeróbica|anaeróbica|fermentação",
            "🧬 **Respiração Celular**\n\nProcesso de obtenção de energia (ATP) a partir de glicose.\n\n**Aeróbica (com O₂):**\nC₆H₁₂O₆ + 6O₂ → 6CO₂ + 6H₂O + **38 ATP**\n\nEtapas: Glicólise (citoplasma) → Ciclo de Krebs (mitocôndria) → Cadeia respiratória\n\n**Anaeróbica (sem O₂):**\n• Fermentação lática: glicose → ácido lático + 2 ATP\n• Fermentação alcoólica: glicose → etanol + CO₂ + 2 ATP");

        conceptDictionary.put("genética|dna|rna|cromossomo|gene|hereditariedade|mendel",
            "🧬 **Genética**\n\n**DNA:** dupla hélice de nucleotídeos (A-T, G-C); armazena informação genética.\n**RNA:** mensageiro (mRNA), transportador (tRNA), ribossomal (rRNA).\n\n**Leis de Mendel:**\n• **1ª Lei (Segregação):** cada caráter é determinado por par de fatores que se separam na formação dos gametas.\n• **2ª Lei (Segregação Independente):** dois pares de fatores se segregam independentemente.\n\n**Dominância:** alelo dominante (D) se expressa sobre o recessivo (r).");

        conceptDictionary.put("evolução|darwin|seleção natural|espécie|adaptação",
            "🧬 **Evolução**\n\n**Teoria de Darwin:**\n• Variação hereditária existe nas populações\n• Competição pelos recursos (luta pela existência)\n• **Seleção natural:** indivíduos mais adaptados sobrevivem e se reproduzem mais\n\n**Conceitos:**\n• **Especiação:** formação de novas espécies por isolamento reprodutivo\n• **Adaptação:** características favoráveis selecionadas ao longo do tempo\n• **Mutação + seleção natural** = motor da evolução");

        conceptDictionary.put("ecologia|ecossistema|cadeia alimentar|bioma|habitat|nicho",
            "🧬 **Ecologia**\n\n**Cadeia alimentar:** produtor → consumidor primário → consumidor secundário → decomposto\n\n**Biomas brasileiros:** Amazônia, Cerrado, Mata Atlântica, Caatinga, Pampa, Pantanal\n\n**Relações ecológicas:**\n• **Mutualismo:** (+/+) — liquen\n• **Comensalismo:** (+/0) — rêmora\n• **Parasitismo:** (+/−) — tênia\n• **Predatismo:** (+/−) — leão/zebra\n\n**Ciclos biogeoquímicos:** C, N, H₂O, P");

        // ── HISTÓRIA ────────────────────────────────────────────────────────────────
        conceptDictionary.put("revolução industrial|industrialização|capitalismo|proletariado",
            "🏛️ **Revolução Industrial**\n\nOcorreu na Inglaterra (século XVIII) e se expandiu para a Europa e América no século XIX.\n\n**Causas:** acúmulo de capital, disponibilidade de carvão, estabilidade política inglesa, enclosures (expulsão do campo).\n\n**Consequências:** urbanização acelerada, surgimento do proletariado, poluição, trabalho infantil, sindicalismo, socialismo.\n\n**Fases:** 1ª (vapor + têxtil), 2ª (aço + eletricidade + petróleo), 3ª (automação + tecnologia).");

        conceptDictionary.put("revolução francesa|iluminismo|liberdade|igualdade|fraternidade",
            "🏛️ **Revolução Francesa (1789)**\n\nRevolução burguesa que derrubou o Antigo Regime na França.\n\n**Causas:** crise financeira, fome, influência iluminista, desigualdade social (3 estados).\n\n**Fases:** Assembleia Constituinte → Assembleia Legislativa → Convenção Nacional (Terror/Robespierre) → Diretório → Golpe de Napoleão (1799).\n\n**Legado:** Declaração dos Direitos do Homem, ideais liberais, queda do absolutismo.");

        conceptDictionary.put("segunda guerra mundial|nazismo|fascismo|holocausto|hitler",
            "🏛️ **Segunda Guerra Mundial (1939-1945)**\n\n**Causas:** ascensão do nazismo (Hitler) e fascismo (Mussolini), crise de 1929, Tratado de Versalhes (humilhação alemã), expansionismo.\n\n**Fatos-chave:** invasão da Polônia (1939), Blitzkrieg, Operação Barbarossa, Pearl Harbor (EUA entram, 1941), D-Day (1944), bombas atômicas em Hiroshima/Nagasaki (1945).\n\n**Consequências:** 60-80 milhões de mortos, Holocausto (6M judeus), ONU, Guerra Fria.");

        conceptDictionary.put("brasil colônia|colonização|escravidão|pau-brasil|capitanias",
            "🏛️ **Brasil Colonial (1500-1822)**\n\n**Fases econômicas:** Pau-Brasil → Açúcar (sec. XVI-XVII) → Mineração (sec. XVIII) → Café (sec. XIX)\n\n**Escravidão:** principal mão de obra; ~4 milhões de africanos trazidos ao Brasil (maior da América).\n\n**Capitanias Hereditárias:** divisão territorial de 1534; maioria fracassou exceto São Vicente e Pernambuco.\n\n**Independência:** 1822, Dom Pedro I; pressão das cortes portuguesas para recolonização.");

        // ── GEOGRAFIA ────────────────────────────────────────────────────────────────
        conceptDictionary.put("globalização|mundialização|neoliberalismo|economia global",
            "🌎 **Globalização**\n\nProcesso de integração econômica, cultural e política entre países, intensificado após 1990 com o fim da Guerra Fria e avanço da internet.\n\n**Aspectos:**\n• **Econômico:** empresas multinacionais, comércio mundial, FMI, OMC\n• **Cultural:** homogeneização cultural, americanização\n• **Tecnológico:** internet, comunicação instantânea\n\n**Críticas:** aumento das desigualdades, desindustrialização de países em desenvolvimento.");

        conceptDictionary.put("clima|temperatura|precipitação|bioma|vegetação",
            "🌎 **Clima e Vegetação**\n\n**Tipos climáticos:** equatorial (quente/úmido), tropical (seco/chuvoso), semiárido, subtropical, temperado, polar.\n\n**Fatores:** latitude, altitude, maritimidade, correntes oceânicas, massas de ar.\n\n**Climas do Brasil:** equatorial (Amazônia), tropical (centro), semiárido (Nordeste), subtropical (Sul).\n\nEl Niño (aquecimento do Pacífico): seca no Norte/Nordeste, chuvas no Sul.");

        conceptDictionary.put("demografia|população|migração|urbanização|densidade",
            "🌎 **Demografia**\n\n**Indicadores:**\n• **Taxa de natalidade:** nascimentos por 1.000 hab./ano\n• **Taxa de mortalidade:** mortes por 1.000 hab./ano\n• **Crescimento vegetativo:** natalidade − mortalidade\n• **IDH:** Índice de Desenvolvimento Humano (educação + renda + saúde)\n\n**Transição demográfica:** queda da mortalidade → queda da natalidade (países desenvolvidos).\n\n**Urbanização brasileira:** acelerada nos anos 1950-1980; hoje >87% da população vive em cidades.");

        // ── INFORMÁTICA / TECNOLOGIA ────────────────────────────────────────────────
        conceptDictionary.put("algoritmo|programação|código|software|lógica de programação",
            "💻 **Algoritmo e Programação**\n\nAlgoritmo é uma sequência finita de instruções para resolver um problema.\n\n**Estruturas fundamentais:**\n• **Sequência:** instruções executadas em ordem\n• **Decisão:** if/else (se/senão)\n• **Repetição:** for, while (para, enquanto)\n\n**Paradigmas:** imperativo, orientado a objetos, funcional.\n\n**Dica:** comece com pseudocódigo (Portugol) antes de uma linguagem real como Python ou Java.");

        conceptDictionary.put("internet|web|http|html|rede",
            "💻 **Internet e Web**\n\n**Internet:** rede mundial de computadores interligados pelo protocolo TCP/IP.\n\n**Web:** serviço da internet baseado em HTTP/HTTPS e HTML.\n\n**Protocolos comuns:**\n• HTTP/HTTPS: páginas web\n• FTP: transferência de arquivos\n• SMTP/IMAP: e-mail\n• DNS: traduz domínio → IP\n\n**Segurança:** HTTPS = HTTP + criptografia TLS/SSL.");
    }

    private void initializeStudyTips() {
        studyTips.put("matemática", List.of(
            "Pratique exercícios diariamente — 30 minutos consistentes superam maratonas esporádicas.",
            "Resolva o problema sem ver a resposta antes. Depois compare e entenda o erro.",
            "Use o método de 'teach-back': explique o conceito em voz alta como se fosse ensinar alguém.",
            "Faça mapas mentais conectando fórmulas relacionadas.",
            "Verifique a resposta substituindo na equação original."
        ));
        studyTips.put("português", List.of(
            "Leia ao menos uma notícia completa por dia — amplia vocabulário e referências para redação.",
            "Escreva um parágrafo argumentativo diariamente sobre qualquer tema.",
            "Ao estudar gramática, crie exemplos próprios — não decore regras abstratas.",
            "Para concordância: identifique sempre o sujeito antes de definir o verbo.",
            "Na redação ENEM, a proposta de intervenção responde: Quem faz? O quê? Como? Para quê?"
        ));
        studyTips.put("física", List.of(
            "Entenda os conceitos antes de decorar fórmulas — a fórmula é consequência do raciocínio.",
            "Desenhe o problema: diagramas de força, gráficos v×t ajudam a visualizar.",
            "Confira as unidades em cada etapa do cálculo (análise dimensional).",
            "Relacione a física com situações do cotidiano: carro, elevador, chuveiro elétrico.",
            "Resolva pelo menos 3 variações de cada tipo de problema."
        ));
        studyTips.put("química", List.of(
            "Aprenda a tabela periódica por grupos/famílias, não decorando elemento por elemento.",
            "Para balancear equações: comece pelos elementos que aparecem menos vezes.",
            "Entenda eletronegatividade antes de estudar ligações e polaridade.",
            "Relacione pH com situações do dia a dia: suco de limão (ácido), sabão (básico).",
            "Use fichas para memorizar nomes e fórmulas dos ácidos e bases mais comuns."
        ));
        studyTips.put("biologia", List.of(
            "Monte esquemas visuais dos processos (fotossíntese, respiração, divisão celular).",
            "Relacione genética com exemplos de família — dominância, recessividade ficam intuitivos.",
            "Para ecologia, pense em cadeias alimentares reais do seu bioma local.",
            "Estude evolução como processo lógico, não como lista de fatos a decorar.",
            "Revisite a célula periodicamente — quase todos os tópicos de bio voltam a ela."
        ));
        studyTips.put("história", List.of(
            "Construa uma linha do tempo visual conectando eventos — causa e consequência são mais importantes que datas exatas.",
            "Para cada evento, pergunte: quem? por quê? quais consequências?",
            "Relacione história com contexto econômico e social — eventos não surgem do nada.",
            "Use mapas para entender deslocamentos, guerras e domínios territoriais.",
            "Filmes e documentários ajudam a contextualizar épocas — mas sempre verifique a precisão histórica."
        ));
        studyTips.put("geografia", List.of(
            "Estude mapas ativamente: identifique biomas, bacias, capitais sem olhar.",
            "Relacione clima com a vida das pessoas e a economia das regiões.",
            "Para geopolítica: entenda interesses econômicos por trás de conflitos e alianças.",
            "Acompanhe notícias sobre desastres naturais, migrações e conflitos — são aulas práticas de geo.",
            "Conecte demografía com economia: natalidade, IDH e urbanização contam uma história."
        ));
    }

    private void initializeSubjects() {
        availableSubjects.addAll(List.of(
            "Matemática", "Português", "Física", "Química",
            "Biologia", "História", "Geografia", "Informática"
        ));
    }

    private String generateGenericAnswer(String question, String context) {
        String subject = (context != null && !context.isBlank()) ? context : detectSubject(question);
        String tips = getStudyTipsForSubject(subject).stream().limit(2)
                .reduce("", (a, b) -> a + "\n• " + b);

        return String.format(
            "Essa é uma boa pergunta sobre **%s**! 🎓\n\n" +
            "Ainda não tenho essa resposta específica na minha base local, mas posso te ajudar assim:\n\n" +
            "**Dicas para estudar %s:**%s\n\n" +
            "Para respostas mais detalhadas, você pode:\n" +
            "• Reformular a pergunta com termos mais específicos (ex: 'o que é derivada?')\n" +
            "• Consultar a seção **Fundamentação Teórica** do RevisaLab\n" +
            "• Pesquisar no Khan Academy (khanacademy.org.br) ou Curso Enem Gratuito\n\n" +
            "Posso ajudar com conceitos de Matemática, Português, Física, Química, Biologia, História ou Geografia. Qual desses quer explorar?",
            question.length() > 60 ? question.substring(0, 57) + "..." : question,
            subject,
            tips.isEmpty() ? "\n• Pratique regularmente e revise o conteúdo anterior." : tips
        );
    }

    private String detectSubject(String question) {
        if (question == null) return "estudos";
        String q = question.toLowerCase();
        if (q.matches(".*\\b(equação|função|derivada|integral|logaritmo|trigonometria|geometria|álgebra|número|matriz|vetor)\\b.*")) return "Matemática";
        if (q.matches(".*\\b(verbo|substantivo|concordância|redação|gramática|texto|pontuação|crase|sujeito|predicado)\\b.*")) return "Português";
        if (q.matches(".*\\b(força|velocidade|aceleração|energia|calor|onda|eletricidade|óptica|termodinâmica)\\b.*")) return "Física";
        if (q.matches(".*\\b(átomo|molécula|elemento|reação|ácido|base|tabela periódica|orgânica|química)\\b.*")) return "Química";
        if (q.matches(".*\\b(célula|dna|gene|fotossíntese|respiração|ecologia|evolução|bioma|vírus|bactéria)\\b.*")) return "Biologia";
        if (q.matches(".*\\b(guerra|revolução|colônia|império|república|feudalismo|capitalismo|socialismo)\\b.*")) return "História";
        if (q.matches(".*\\b(clima|bioma|população|migração|continente|país|mapa|relevo|globalização)\\b.*")) return "Geografia";
        return "estudos";
    }

    private List<String> getGenericTips() {
        return List.of(
            "Estude em blocos de 25-30 minutos (técnica Pomodoro) com pausas de 5 minutos.",
            "Revise o conteúdo no dia seguinte, em 1 semana e em 1 mês (repetição espaçada).",
            "Durma bem — a consolidação da memória acontece durante o sono.",
            "Explique em voz alta o que aprendeu; se não conseguir, ainda não entendeu."
        );
    }
}
