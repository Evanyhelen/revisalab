package com.revisalab.config;

import com.revisalab.enums.ERole;
import com.revisalab.model.*;
import com.revisalab.model.Module;
import com.revisalab.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Carrega dados iniciais no banco em memória na primeira execução.
 */
@Component
public class DataLoader implements CommandLineRunner {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TrailRepository trailRepository;
    @Autowired private AchievementRepository achievementRepository;
    @Autowired private MissionRepository missionRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private ExerciseRepository exerciseRepository;
    @Autowired private AnswerOptionRepository answerOptionRepository;

    @Override
    public void run(String... args) {
        loadRoles();
        loadAdminUser();
        List<Trail> trails = loadSampleTrails();
        loadModulesAndExercises(trails);
        loadAchievements();
        loadMissions();
        classifyExistingAccounts();
    }

    private void loadRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.saveAll(Arrays.asList(
                    new Role(ERole.ROLE_USER),
                    new Role(ERole.ROLE_ADMIN)
            ));
        }
    }

    private void loadAdminUser() {
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = new User();
            admin.setName("Felipe");
            admin.setEmail("admin@revisalab.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setXp(1000);
            admin.setLevel(2);
            admin.setStreak(7);
            admin.setLastActivity(LocalDateTime.now());
            admin.setRoles(new HashSet<>(Arrays.asList(adminRole)));
            admin.setIsDemo(true);
            userRepository.save(admin);

            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("User role not found"));

            User testUser = new User();
            testUser.setName("Bianca");
            testUser.setEmail("teste@revisalab.com");
            testUser.setPassword(passwordEncoder.encode("teste123"));
            testUser.setXp(250);
            testUser.setLevel(1);
            testUser.setStreak(3);
            testUser.setLastActivity(LocalDateTime.now());
            testUser.setRoles(new HashSet<>(Arrays.asList(userRole)));
            testUser.setIsDemo(true);
            userRepository.save(testUser);
        }
    }

    private List<Trail> loadSampleTrails() {
        if (trailRepository.count() > 0) {
            return trailRepository.findAll();
        }

        List<Trail> trails = new ArrayList<>();
        trails.add(createTrail("Matemática Básica",
                "Conceitos fundamentais de matemática do ensino médio",
                "Matemática", 2, "calculate", "#3f51b5", 10));
        trails.add(createTrail("Português Essencial",
                "Gramática, interpretação e redação",
                "Português", 1, "edit_note", "#f44336", 8));
        trails.add(createTrail("Física Fundamental",
                "Conceitos básicos de física para ensino superior",
                "Física", 3, "science", "#4caf50", 12));
        trails.add(createTrail("Química Básica",
                "Estrutura da matéria, ligações e reações",
                "Química", 2, "biotech", "#16a34a", 9));
        trails.add(createTrail("Raciocínio Lógico",
                "Lógica proposicional, sequências e dedução",
                "Raciocínio Lógico", 2, "psychology", "#f59e0b", 6));
        trails.add(createTrail("História do Brasil",
                "Do período colonial à república",
                "História", 1, "history_edu", "#dc2626", 10));
        trails.add(createTrail("Geografia Geral",
                "Geografia física, humana e do Brasil",
                "Geografia", 1, "public", "#0891b2", 8));
        trails.add(createTrail("Biologia Celular",
                "Célula, metabolismo e genética básica",
                "Biologia", 2, "eco", "#65a30d", 9));
        trails.add(createTrail("Redação Nota 1000",
                "Estrutura dissertativa, coesão e argumentação para a redação do ENEM",
                "Redação", 2, "edit_note", "#e11d48", 10));
        trails.add(createTrail("Filosofia e Ética",
                "Correntes filosóficas, ética e pensadores clássicos",
                "Filosofia", 2, "psychology", "#7c3aed", 8));
        trails.add(createTrail("Sociologia e Cidadania",
                "Estado, sociedade, cultura e direitos do cidadão",
                "Sociologia", 2, "groups", "#0ea5e9", 8));
        trails.add(createTrail("Inglês Instrumental",
                "Leitura, vocabulário e interpretação de textos em inglês",
                "Inglês", 2, "translate", "#2563eb", 8));
        trails.add(createTrail("Geometria e Trigonometria",
                "Geometria plana, teorema de Pitágoras e razões trigonométricas",
                "Matemática", 3, "architecture", "#4338ca", 10));
        trails.add(createTrail("Literatura e Leitura",
                "Escolas literárias, autores brasileiros e estratégias de leitura",
                "Português", 2, "auto_stories", "#b91c1c", 8));
        trails.add(createTrail("Óptica e Ondas",
                "Ondas, som, luz, reflexão e refração",
                "Física", 3, "flare", "#047857", 9));
        trails.add(createTrail("Química Orgânica",
                "Funções orgânicas, isomeria, reações e polímeros",
                "Química", 3, "science", "#7c3aed", 9));

        trailRepository.saveAll(trails);
        return trails;
    }

    private Trail createTrail(String title, String description, String category,
                              int difficulty, String icon, String color, int estimatedHours) {
        Trail trail = new Trail();
        trail.setTitle(title);
        trail.setDescription(description);
        trail.setCategory(category);
        trail.setDifficulty(difficulty);
        trail.setIcon(icon);
        trail.setColor(color);
        trail.setEstimatedHours(estimatedHours);
        return trail;
    }

    private void loadModulesAndExercises(List<Trail> trails) {
        if (moduleRepository.count() > 0 || trails.isEmpty()) {
            return;
        }

        for (Trail trail : trails) {
            switch (trail.getTitle()) {
                case "Matemática Básica" -> seedMathModules(trail);
                case "Geometria e Trigonometria" -> seedGeometryModules(trail);
                case "Português Essencial" -> seedPortugueseModules(trail);
                case "Literatura e Leitura" -> seedLiteratureModules(trail);
                case "Física Fundamental" -> seedPhysicsModules(trail);
                case "Óptica e Ondas" -> seedOpticsModules(trail);
                case "Química Básica" -> seedChemistryModules(trail);
                case "Química Orgânica" -> seedOrganicChemistryModules(trail);
                case "Raciocínio Lógico" -> seedLogicModules(trail);
                case "História do Brasil" -> seedHistoryModules(trail);
                case "Geografia Geral" -> seedGeographyModules(trail);
                case "Biologia Celular" -> seedBiologyModules(trail);
                case "Redação Nota 1000" -> seedRedacaoModules(trail);
                case "Filosofia e Ética" -> seedFilosofiaModules(trail);
                case "Sociologia e Cidadania" -> seedSociologiaModules(trail);
                case "Inglês Instrumental" -> seedInglesModules(trail);
                default -> { }
            }
            trail.updateCounts();
            trailRepository.save(trail);
        }
    }

    private void seedMathModules(Trail trail) {
        Module numeros = createModule(trail, "Números e Operações",
                "Operações básicas, frações e equivalências", 1, 30);
        addMC(numeros, "Quanto é 7 × 8?",
                "A multiplicação 7 × 8 resulta em 56.", "B", 1,
                "54", "56", "48", "64");
        addOpenEnded(numeros, "Resolva: 15 + 27", "42", "Somar 15 com 27 resulta em 42.", 1);
        addMC(numeros, "Qual fração é equivalente a 1/2?",
                "2/4 simplifica para 1/2, portanto é equivalente.", "A", 1,
                "2/4", "1/3", "2/5", "3/6");
        addMC(numeros, "Quanto é 12 ÷ 4 + 5 × 2?",
                "Respeitando a ordem das operações: 12 ÷ 4 = 3 e 5 × 2 = 10, logo 3 + 10 = 13.", "A", 2,
                "13", "11", "17", "7");
        addMC(numeros, "Qual é a raiz quadrada de 144?",
                "12 × 12 = 144, portanto √144 = 12.", "A", 1,
                "12", "14", "10", "16");

        Module algebra = createModule(trail, "Álgebra e Equações",
                "Equações de primeiro grau e introdução a funções", 2, 40);
        addOpenEnded(algebra, "Resolva a equação 2x + 5 = 13", "4",
                "Subtraindo 5 dos dois lados: 2x = 8, logo x = 4.", 2);
        addMC(algebra, "Qual é a derivada de f(x) = x²?",
                "Pela regra da potência, a derivada de x² é 2x.", "A", 3,
                "2x", "x", "2", "x²");
        addMC(algebra, "Se 3x = 15, quanto vale x?",
                "Dividindo os dois lados por 3: x = 5.", "A", 1,
                "5", "3", "12", "45");

        Module geometria = createModule(trail, "Geometria e Medidas",
                "Área, perímetro e figuras planas", 3, 35);
        addMC(geometria, "Qual é a área de um quadrado de lado 6 cm?",
                "Área = lado² = 6² = 36 cm².", "B", 2,
                "24 cm²", "36 cm²", "12 cm²", "30 cm²");
        addMC(geometria, "Qual é o perímetro de um retângulo de 8 cm por 5 cm?",
                "Perímetro = 2 × (8 + 5) = 2 × 13 = 26 cm.", "A", 1,
                "26 cm", "40 cm", "13 cm", "80 cm");
    }

    private void seedPortugueseModules(Trail trail) {
        Module classes = createModule(trail, "Classes de Palavras",
                "Substantivos, verbos e adjetivos", 1, 25);
        addMC(classes, "Qual destas palavras é um substantivo?",
                "Substantivo nomeia seres e objetos. \"Casa\" é um substantivo comum.", "C", 1,
                "Correr", "Bonito", "Casa", "Rapidamente");
        addMC(classes, "Qual destas palavras é um verbo?",
                "Verbo indica ação, estado ou fenômeno. \"Correr\" é um verbo.", "B", 1,
                "Casa", "Correr", "Bonito", "Azul");
        addMC(classes, "Qual destas palavras é um adjetivo?",
                "Adjetivo caracteriza o substantivo. \"Bonito\" é um adjetivo.", "A", 1,
                "Bonito", "Correr", "Casa", "Rapidamente");

        Module sintaxe = createModule(trail, "Sintaxe da Oração",
                "Sujeito, predicado e análise sintática", 2, 30);
        addOpenEnded(sintaxe, "Na frase 'O gato subiu no telhado', qual é o sujeito?",
                "O gato", "O sujeito é o termo sobre o qual o verbo predica: \"O gato\".", 1);
        addMC(sintaxe, "Na frase 'O gato subiu no telhado', como se classifica 'no telhado'?",
                "Indica o lugar onde ocorre a ação, portanto é adjunto adverbial de lugar.", "B", 2,
                "Objeto direto", "Adjunto adverbial de lugar", "Sujeito", "Predicativo do sujeito");

        Module interpretacao = createModule(trail, "Interpretação de Texto",
                "Ideia central, figuras de linguagem e coesão", 3, 30);
        addMC(interpretacao, "O que é a ideia principal de um texto?",
                "É a mensagem central que o autor quer transmitir ao leitor.", "A", 1,
                "A mensagem central que o autor quer transmitir",
                "O detalhe mais curto do texto",
                "A opinião pessoal do leitor",
                "O último parágrafo do texto");
        addMC(interpretacao, "Qual figura de linguagem há em 'Ele é um leão em campo'?",
                "Há uma comparação implícita (sem 'como'), característica da metáfora.", "C", 2,
                "Comparação", "Metonímia", "Metáfora", "Hipérbole");
    }

    private void seedPhysicsModules(Trail trail) {
        Module cinematica = createModule(trail, "Cinemática",
                "Movimento, velocidade e aceleração", 1, 35);
        addMC(cinematica, "Qual é a fórmula da velocidade média?",
                "Velocidade média é a razão entre deslocamento e tempo: v = Δs/Δt.", "A", 1,
                "v = Δs/Δt", "v = m·a", "v = Δt/Δs", "v = a·t");
        addOpenEnded(cinematica, "Um carro percorre 100 km em 2 h. Qual é a velocidade média?",
                "50 km/h", "v = 100 km / 2 h = 50 km/h.", 2);
        addMC(cinematica, "Qual é a unidade de velocidade no Sistema Internacional?",
                "No SI, a velocidade é medida em metros por segundo (m/s).", "B", 1,
                "km/h", "m/s", "m/s²", "km/min");
        addMC(cinematica, "O que é aceleração?",
                "Aceleração é a variação da velocidade em relação ao tempo.", "A", 2,
                "Variação da velocidade pelo tempo",
                "Distância percorrida pelo tempo",
                "Massa multiplicada pela velocidade",
                "Força dividida pela área");

        Module dinamica = createModule(trail, "Dinâmica",
                "Leis de Newton e força", 2, 40);
        addMC(dinamica, "A Segunda Lei de Newton é expressa por:",
                "A força resultante é o produto da massa pela aceleração: F = m·a.", "A", 2,
                "F = m·a", "F = m/a", "F = a/m", "F = m·v");
        addMC(dinamica, "Qual é a unidade de força no Sistema Internacional?",
                "A força é medida em newton (N), em homenagem a Isaac Newton.", "C", 1,
                "Joule (J)", "Watt (W)", "Newton (N)", "Pascal (Pa)");
        addMC(dinamica, "Qual é o peso de um corpo de 10 kg (g = 10 m/s²)?",
                "Peso = m·g = 10 × 10 = 100 N.", "B", 2,
                "10 N", "100 N", "1000 N", "1 N");
    }

    private void seedChemistryModules(Trail trail) {
        Module geral = createModule(trail, "Química Geral",
                "Estrutura atômica, ligações e soluções", 1, 40);
        addMC(geral, "Qual é o número atômico do oxigênio?",
                "O oxigênio (O) tem número atômico 8, ou seja, 8 prótons no núcleo.", "C", 1,
                "6", "7", "8", "16");
        addMC(geral, "Qual é a fórmula química da água?",
                "A água é formada por dois átomos de hidrogênio e um de oxigênio: H₂O.", "A", 1,
                "H₂O", "CO₂", "O₂", "NaCl");
        addMC(geral, "O que caracteriza uma ligação iônica?",
                "Na ligação iônica ocorre transferência de elétrons entre os átomos.", "B", 2,
                "Compartilhamento de elétrons",
                "Transferência de elétrons",
                "Atração entre moléculas neutras",
                "Repulsão entre prótons");
        addMC(geral, "Qual é o estado físico da água a 25 °C e 1 atm?",
                "Nessas condições (temperatura e pressão ambientes), a água é líquida.", "A", 1,
                "Líquido", "Sólido", "Gasoso", "Plasma");
        addMC(geral, "Qual é o pH de uma solução neutra?",
                "Soluções neutras possuem pH igual a 7.", "C", 1,
                "0", "14", "7", "1");
    }

    private void seedLogicModules(Trail trail) {
        Module proposicional = createModule(trail, "Lógica Proposicional",
                "Condicionais, sequências e dedução", 1, 30);
        addMC(proposicional, "Se todos os A são B e todos os B são C, então:",
                "Por transitividade, A ⊆ B e B ⊆ C implica A ⊆ C. Logo, todos os A são C.", "C", 2,
                "Todos os C são A", "Alguns C são A", "Todos os A são C", "Nenhum C é A");
        addMC(proposicional, "Qual é o próximo número da sequência 2, 4, 8, 16, ...?",
                "Cada termo é o dobro do anterior: 16 × 2 = 32.", "A", 1,
                "32", "24", "30", "64");
        addMC(proposicional, "Se P → Q e P é verdadeiro, o que se conclui?",
                "Pelo modus ponens, se P implica Q e P é verdadeiro, então Q é verdadeiro.", "B", 2,
                "P é falso", "Q é verdadeiro", "Q é falso", "Nada se conclui");
        addMC(proposicional, "Quantos quadrados há em um tabuleiro 2×2 (todos os tamanhos)?",
                "São 4 quadrados 1×1 e 1 quadrado 2×2, totalizando 5.", "B", 2,
                "4", "5", "6", "9");
        addMC(proposicional, "Qual número completa a sequência 3, 6, 12, 24, __?",
                "Cada termo é o dobro do anterior: 24 × 2 = 48.", "A", 1,
                "48", "36", "30", "42");
    }

    private void seedHistoryModules(Trail trail) {
        Module brasil = createModule(trail, "História do Brasil",
                "Período colonial, império e república", 1, 40);
        addMC(brasil, "Em que ano foi proclamada a Independência do Brasil?",
                "A Independência do Brasil foi proclamada por Dom Pedro I em 1822.", "B", 1,
                "1500", "1822", "1889", "1808");
        addMC(brasil, "Quem foi o primeiro presidente do Brasil?",
                "Marechal Deodoro da Fonseca foi o primeiro presidente após a Proclamação da República.", "C", 1,
                "Getúlio Vargas", "Dom Pedro II", "Deodoro da Fonseca", "Juscelino Kubitschek");
        addMC(brasil, "Qual evento marcou o fim da monarquia no Brasil?",
                "A Proclamação da República, em 1889, encerrou o período monárquico.", "A", 1,
                "Proclamação da República",
                "Independência do Brasil",
                "Abolição da Escravatura",
                "Revolução de 1930");
        addMC(brasil, "Em que ano foi assinada a Lei Áurea?",
                "A Lei Áurea, que aboliu a escravidão, foi assinada em 13 de maio de 1888.", "D", 2,
                "1822", "1850", "1871", "1888");
        addMC(brasil, "Qual período é conhecido como 'República Velha'?",
                "A República Velha corresponde ao período de 1889 a 1930.", "B", 2,
                "1500–1822", "1889–1930", "1930–1945", "1945–1964");
    }

    private void seedGeographyModules(Trail trail) {
        Module fisica = createModule(trail, "Geografia Física",
                "Relevo, clima, hidrografia e cartografia", 1, 35);
        addMC(fisica, "Qual é o maior oceano do planeta?",
                "O Oceano Pacífico é o maior, cobrindo cerca de um terço da superfície terrestre.", "A", 1,
                "Pacífico", "Atlântico", "Índico", "Ártico");
        addMC(fisica, "Qual é a capital do Brasil?",
                "Brasília é a capital federal do Brasil desde 1960.", "B", 1,
                "Rio de Janeiro", "Brasília", "São Paulo", "Salvador");
        addMC(fisica, "Qual é o maior rio do mundo em volume de água?",
                "O rio Amazonas é o maior em volume de água, descarregando no Atlântico.", "C", 1,
                "Nilo", "Mississipi", "Amazonas", "Yangtzé");
        addMC(fisica, "O que é o efeito estufa?",
                "É a retenção de calor na atmosfera causada por gases como o CO₂.", "A", 2,
                "Retenção de calor na atmosfera",
                "Resfriamento global",
                "Destruição da camada de ozônio",
                "Chuva ácida");
        addMC(fisica, "Qual é o ponto mais alto do Brasil?",
                "O Pico da Neblina, na Serra do Imeri (AM), é o ponto mais alto do Brasil.", "D", 2,
                "Pão de Açúcar", "Monte Roraima", "Serra do Mar", "Pico da Neblina");
    }

    private void seedBiologyModules(Trail trail) {
        Module celular = createModule(trail, "Biologia Celular",
                "Estrutura celular, organelas e metabolismo", 1, 35);
        addMC(celular, "Qual é a unidade básica da vida?",
                "A célula é a unidade estrutural e funcional dos seres vivos.", "A", 1,
                "Célula", "Átomo", "Molécula", "Tecido");
        addMC(celular, "Qual organela é responsável pela fotossíntese?",
                "O cloroplasto contém clorofila e realiza a fotossíntese nas plantas.", "C", 1,
                "Mitocôndria", "Núcleo", "Cloroplasto", "Ribossomo");
        addMC(celular, "Qual é a função das mitocôndrias?",
                "As mitocôndrias produzem energia (ATP) por meio da respiração celular.", "B", 1,
                "Produzir proteínas", "Produzir energia (ATP)", "Armazenar DNA", "Fazer fotossíntese");
        addMC(celular, "Qual tipo sanguíneo é considerado doador universal?",
                "O tipo O− pode doar para todos os grupos, pois não possui antígenos A, B ou Rh.", "D", 2,
                "A+", "AB+", "O+", "O-");
        addMC(celular, "O que caracteriza uma célula eucariótica?",
                "Células eucarióticas possuem núcleo definido, envolvido por membrana.", "A", 1,
                "Célula com núcleo definido",
                "Célula sem núcleo",
                "Um tipo de vírus",
                "Um tipo de bactéria");
    }

    private void seedRedacaoModules(Trail trail) {
        Module estrutura = createModule(trail, "Estrutura da Redação",
                "Tese, desenvolvimento e conclusão", 1, 30);
        addMC(estrutura, "Qual é a estrutura clássica de um texto dissertativo-argumentativo?",
                "A dissertação-argumentativa é organizada em introdução, desenvolvimento e conclusão.", "A", 1,
                "Introdução, desenvolvimento e conclusão",
                "Título, resumo e anexo",
                "Narrador, enredo e clímax",
                "Verso, estrofe e rima");
        addMC(estrutura, "O que é a tese em uma redação?",
                "A tese é a opinião central que o autor defende ao longo do texto.", "B", 2,
                "O título criativo do texto",
                "A opinião central a ser defendida",
                "O último parágrafo do texto",
                "Um sinônimo de introdução");

        Module argumentacao = createModule(trail, "Argumentação e Coesão",
                "Conectivos e progressão textual", 2, 30);
        addMC(argumentacao, "Qual conectivo expressa conclusão?",
                "\"Portanto\" introduz uma conclusão lógica do que foi exposto.", "C", 1,
                "Todavia", "Embora", "Portanto", "Outrossim");
        addMC(argumentacao, "O que é coesão textual?",
                "Coesão é a conexão lógica e gramatical entre as partes do texto.", "D", 1,
                "O uso de palavras difíceis",
                "O tamanho dos parágrafos",
                "A repetição de ideias",
                "A conexão lógica entre as partes do texto");
    }

    private void seedFilosofiaModules(Trail trail) {
        Module classicos = createModule(trail, "Pensadores Clássicos",
                "Sócrates, Platão e Aristóteles", 1, 30);
        addMC(classicos, "A alegoria (mito) da caverna é atribuída a qual filósofo?",
                "Platão criou a alegoria da caverna para explicar a teoria das ideias.", "B", 2,
                "Sócrates", "Platão", "Aristóteles", "Descartes");
        addMC(classicos, "O método da maiêutica (parto das ideias) é associado a:",
                "Sócrates usava perguntas para conduzir o interlocutor à verdade.", "A", 2,
                "Sócrates", "Platão", "Kant", "Nietzsche");
        addMC(classicos, "A frase 'Penso, logo existo' é atribuída a:",
                "René Descartes sintetizou o racionalismo com o 'cogito'.", "C", 1,
                "Aristóteles", "Platão", "Descartes", "Sartre");

        Module etica = createModule(trail, "Ética e Moral",
                "Valores, dever e conduta humana", 2, 30);
        addMC(etica, "O que a ética estuda?",
                "A ética investiga os valores morais e os princípios da conduta humana.", "A", 1,
                "Os valores morais e a conduta humana",
                "A estrutura dos átomos",
                "As leis da física",
                "A gramática da língua");
    }

    private void seedSociologiaModules(Trail trail) {
        Module classicos = createModule(trail, "Clássicos da Sociologia",
                "Comte, Durkheim, Marx e Weber", 1, 35);
        addMC(classicos, "Quem é considerado o 'pai da Sociologia'?",
                "Auguste Comte cunhou o termo 'sociologia' e a propôs como ciência.", "B", 1,
                "Karl Marx", "Auguste Comte", "Max Weber", "Émile Durkheim");
        addMC(classicos, "O conceito de 'fato social' é central na obra de:",
                "Durkheim definiu o fato social como exterior, coercitivo e geral.", "D", 2,
                "Karl Marx", "Max Weber", "Auguste Comte", "Émile Durkheim");
        addMC(classicos, "O conceito de 'luta de classes' é central na obra de:",
                "Karl Marx analisou a história a partir do conflito entre classes sociais.", "A", 1,
                "Karl Marx", "Max Weber", "Auguste Comte", "Émile Durkheim");

        Module cidadania = createModule(trail, "Cidadania e Direitos",
                "Estado, sociedade e participação", 2, 25);
        addMC(cidadania, "O que é cidadania?",
                "Cidadania é o conjunto de direitos e deveres do indivíduo em sociedade.", "C", 1,
                "O valor de um imposto",
                "Um tipo de governo",
                "O conjunto de direitos e deveres do indivíduo",
                "A capital de um país");
    }

    private void seedInglesModules(Trail trail) {
        Module leitura = createModule(trail, "Leitura e Interpretação",
                "Compreensão textual e vocabulário", 1, 30);
        addMC(leitura, "O que significa 'therefore' em português?",
                "\"Therefore\" é um conectivo de conclusão: 'portanto'.", "B", 1,
                "Porém", "Portanto", "Embora", "Depois");
        addMC(leitura, "Na frase 'The book was written by Machado de Assis', a voz verbal é:",
                "A construção 'was written' é voz passiva (o sujeito sofre a ação).", "A", 2,
                "Passiva", "Ativa", "Reflexiva", "Imperativa");
        addMC(leitura, "O que é um 'false cognate' (falso cognato)?",
                "Falso cognato é uma palavra semelhante ao português, mas com sentido diferente.", "C", 2,
                "Palavra com o mesmo sentido em dois idiomas",
                "Palavra sem tradução",
                "Palavra parecida, mas com sentido diferente",
                "Palavra de origem latina");

        Module vocabulario = createModule(trail, "Vocabulário em Contexto",
                "Palavras frequentes em provas", 2, 25);
        addMC(vocabulario, "O advérbio 'actually' significa:",
                "\"Actually\" significa 'na verdade', não 'atualmente'.", "D", 1,
                "Atualmente", "Imediatamente", "Finalmente", "Na verdade");
    }

    private void seedGeometryModules(Trail trail) {
        Module plana = createModule(trail, "Geometria Plana",
                "Triângulos, áreas e circunferência", 1, 35);
        addMC(plana, "Qual é a soma dos ângulos internos de um triângulo?",
                "A soma dos ângulos internos de qualquer triângulo é sempre 180°.", "C", 1,
                "90°", "360°", "180°", "270°");
        addMC(plana, "Qual é a área de um triângulo de base 10 cm e altura 6 cm?",
                "Área = (base × altura) / 2 = (10 × 6) / 2 = 30 cm².", "B", 1,
                "60 cm²", "30 cm²", "16 cm²", "15 cm²");
        addMC(plana, "O comprimento de uma circunferência de raio r é dado por:",
                "O comprimento da circunferência é C = 2πr.", "A", 2,
                "C = 2πr", "C = πr²", "C = r²", "C = 2r");

        Module trigonometria = createModule(trail, "Trigonometria",
                "Razões trigonométricas e Pitágoras", 2, 40);
        addMC(trigonometria, "Em um triângulo retângulo, o seno de um ângulo agudo é a razão entre:",
                "Seno = cateto oposto / hipotenusa.", "B", 2,
                "cateto adjacente / hipotenusa",
                "cateto oposto / hipotenusa",
                "cateto oposto / cateto adjacente",
                "hipotenusa / cateto oposto");
        addMC(trigonometria, "Qual é o valor de sen 30°?",
                "O seno de 30° vale 1/2 (ou 0,5).", "A", 1,
                "1/2", "√2/2", "√3/2", "1");
        addOpenEnded(trigonometria, "Em um triângulo retângulo de catetos 3 e 4, quanto vale a hipotenusa?",
                "5", "Pelo Teorema de Pitágoras: h² = 3² + 4² = 25, logo h = 5.", 2);
    }

    private void seedLiteratureModules(Trail trail) {
        Module escolas = createModule(trail, "Escolas Literárias",
                "Romantismo, Realismo e Modernismo", 1, 35);
        addMC(escolas, "Qual escola literária brasileira é marcada pelo nacionalismo e pelo 'herói índio'?",
                "O Romantismo valorizou o nacionalismo e idealizou o indígena (Iracema, O Guarani).", "A", 2,
                "Romantismo", "Realismo", "Parnasianismo", "Barroco");
        addMC(escolas, "A obra 'Dom Casmurro' é de qual autor?",
                "Machado de Assis escreveu Dom Casmurro, clássico do Realismo brasileiro.", "C", 1,
                "José de Alencar", "Graciliano Ramos", "Machado de Assis", "Carlos Drummond de Andrade");
        addMC(escolas, "Qual movimento teve como marco a Semana de Arte Moderna de 1922?",
                "A Semana de 22 inaugurou o Modernismo brasileiro.", "B", 1,
                "Realismo", "Modernismo", "Naturalismo", "Simbolismo");

        Module leitura = createModule(trail, "Leitura e Inferência",
                "Tipos de narrador e estratégias de compreensão", 2, 30);
        addMC(leitura, "O narrador onisciente é aquele que:",
                "O narrador onisciente conhece os pensamentos e sentimentos das personagens.", "D", 2,
                "Participa da história como personagem",
                "Conta apenas o que vê",
                "Usa a primeira pessoa",
                "Conhece os pensamentos das personagens");
        addMC(leitura, "Qual recurso emprega as palavras em sentido figurado?",
                "A metáfora (figura de linguagem) usa as palavras em sentido figurado.", "A", 1,
                "Figuras de linguagem", "Concordância verbal", "Ortografia", "Pontuação");
        addMC(leitura, "Um texto injuntivo tem como principal objetivo:",
                "O texto injuntivo orienta o leitor a realizar uma ação (receitas, manuais).", "C", 2,
                "Contar uma história",
                "Descrever um cenário",
                "Orientar uma ação",
                "Defender uma opinião");
    }

    private void seedOpticsModules(Trail trail) {
        Module ondas = createModule(trail, "Ondas e Som",
                "Frequência, velocidade e natureza das ondas", 1, 35);
        addMC(ondas, "O que é a frequência de uma onda?",
                "Frequência é o número de oscilações (ciclos) por segundo, medida em hertz.", "B", 1,
                "A altura da onda",
                "O número de oscilações por segundo",
                "A distância entre dois vales",
                "A velocidade de propagação");
        addMC(ondas, "A velocidade de propagação de uma onda é dada por:",
                "A equação fundamental da ondulatória é v = λ·f.", "A", 2,
                "v = λ·f", "v = λ/f", "v = f/λ", "v = λ + f");
        addMC(ondas, "O som é uma onda de natureza:",
                "O som é uma onda mecânica e longitudinal (precisa de meio e vibra na direção da propagação).", "C", 1,
                "Eletromagnética e transversal",
                "Mecânica e transversal",
                "Mecânica e longitudinal",
                "Eletromagnética e longitudinal");

        Module optica = createModule(trail, "Óptica",
                "Luz, reflexão e refração", 2, 35);
        addMC(optica, "A velocidade da luz no vácuo é aproximadamente:",
                "A luz viaja no vácuo a cerca de 3×10⁸ m/s (300 000 km/s).", "D", 1,
                "3×10⁶ m/s", "3×10⁷ m/s", "3×10¹⁰ m/s", "3×10⁸ m/s");
        addMC(optica, "Qual fenômeno separa a luz branca nas cores do arco-íris?",
                "A dispersão (refração com separação das cores) forma o arco-íris.", "B", 2,
                "Reflexão", "Dispersão", "Absorção", "Difração");
        addMC(optica, "Em um espelho plano, a imagem formada é:",
                "O espelho plano forma imagem virtual, direita e de mesmo tamanho do objeto.", "A", 2,
                "Virtual e de mesmo tamanho", "Real e invertida", "Virtual e ampliada", "Real e menor");
    }

    private void seedOrganicChemistryModules(Trail trail) {
        Module funcoes = createModule(trail, "Funções Orgânicas",
                "Grupos funcionais e hidrocarbonetos", 1, 40);
        addMC(funcoes, "Qual grupo funcional caracteriza os álcoois?",
                "Os álcoois possuem a hidroxila (-OH) ligada a carbono saturado.", "C", 2,
                "-COOH", "-NH₂", "-OH", "-CHO");
        addMC(funcoes, "Os hidrocarbonetos são compostos formados apenas por:",
                "Hidrocarbonetos contêm somente carbono e hidrogênio.", "A", 1,
                "Carbono e hidrogênio", "Carbono e oxigênio", "Carbono e nitrogênio", "Hidrogênio e oxigênio");
        addMC(funcoes, "O etanol (C₂H₅OH) pertence à função:",
                "O etanol possui a hidroxila (-OH) e, portanto, é um álcool.", "B", 1,
                "Ácido carboxílico", "Álcool", "Éter", "Cetona");

        Module reacoes = createModule(trail, "Reações e Isomeria",
                "Ésteres, polímeros e isômeros", 2, 30);
        addMC(reacoes, "A reação entre um ácido carboxílico e um álcool forma:",
                "A esterificação produz um éster e água.", "D", 2,
                "Éter", "Aldeído", "Amida", "Éster");
        addMC(reacoes, "O que é isomeria?",
                "Isômeros têm a mesma fórmula molecular, mas estruturas diferentes.", "A", 2,
                "Mesma fórmula molecular e estruturas diferentes",
                "Fórmulas diferentes e mesma estrutura",
                "Mesma estrutura e mesmo nome",
                "Diferentes elementos químicos");
        addMC(reacoes, "Os polímeros são formados pela repetição de:",
                "Polímeros são macromoléculas formadas pela união de monômeros.", "C", 1,
                "Átomos isolados", "Íons metálicos", "Monômeros", "Moléculas de água");
    }

    private Module createModule(Trail trail, String title, String description,
                                int orderNumber, int estimatedMinutes) {
        Module module = new Module();
        module.setTrail(trail);
        module.setTitle(title);
        module.setDescription(description);
        module.setOrderNumber(orderNumber);
        module.setEstimatedMinutes(estimatedMinutes);
        module.setTotalExercises(0);
        module = moduleRepository.save(module);
        trail.getModules().add(module);
        return module;
    }

    private Exercise addMC(Module module, String question, String explanation,
                           String correctLetter, int difficulty, String... options) {
        Exercise exercise = createExercise(module, question, "multiple-choice",
                explanation, correctLetter, difficulty);
        for (int i = 0; i < options.length; i++) {
            answerOptionRepository.save(new AnswerOption(exercise, options[i], i));
        }
        return exercise;
    }

    private Exercise addOpenEnded(Module module, String question, String correctAnswer,
                                  String explanation, int difficulty) {
        return createExercise(module, question, "open-ended", explanation, correctAnswer, difficulty);
    }

    private Exercise createExercise(Module module, String question, String type,
                                    String explanation, String correctAnswer, int difficulty) {
        Exercise exercise = new Exercise();
        exercise.setModule(module);
        exercise.setQuestion(question);
        exercise.setType(type);
        exercise.setExplanation(explanation);
        exercise.setCorrectAnswer(correctAnswer);
        exercise.setDifficulty(difficulty);
        exercise.setXpReward(10);
        exercise = exerciseRepository.save(exercise);
        module.getExercises().add(exercise);
        module.setTotalExercises(module.getExercises().size());
        moduleRepository.save(module);
        return exercise;
    }

    private void loadAchievements() {
        if (achievementRepository.count() == 0) {
            Achievement streak3 = new Achievement();
            streak3.setCode("STREAK_3");
            streak3.setName("Estudante Consistente");
            streak3.setDescription("Mantenha um streak de 3 dias consecutivos");
            streak3.setType("streak");
            streak3.setRequiredValue(3);
            streak3.setXpReward(50);
            streak3.setIcon("local_fire_department");
            achievementRepository.save(streak3);

            Achievement streak7 = new Achievement();
            streak7.setCode("STREAK_7");
            streak7.setName("Estudante Dedicado");
            streak7.setDescription("Mantenha um streak de 7 dias consecutivos");
            streak7.setType("streak");
            streak7.setRequiredValue(7);
            streak7.setXpReward(150);
            streak7.setIcon("emoji_events");
            achievementRepository.save(streak7);

            Achievement level5 = new Achievement();
            level5.setCode("LEVEL_5");
            level5.setName("Mestre em Formação");
            level5.setDescription("Alcance o nível 5");
            level5.setType("level");
            level5.setRequiredValue(5);
            level5.setXpReward(200);
            level5.setIcon("school");
            achievementRepository.save(level5);

            Achievement exercises50 = new Achievement();
            exercises50.setCode("EXERCISES_50");
            exercises50.setName("Resolvedor de Problemas");
            exercises50.setDescription("Complete 50 exercícios");
            exercises50.setType("exercise");
            exercises50.setRequiredValue(50);
            exercises50.setXpReward(100);
            exercises50.setIcon("assignment_turned_in");
            achievementRepository.save(exercises50);
        }
    }

    private void loadMissions() {
        if (missionRepository.count() == 0) {
            Mission daily5 = new Mission();
            daily5.setCode("DAILY_5_EXERCISES");
            daily5.setTitle("Resolva 5 Exercícios");
            daily5.setDescription("Complete 5 exercícios em qualquer disciplina");
            daily5.setType("daily");
            daily5.setActionType("complete_exercises");
            daily5.setRequiredValue(5);
            daily5.setXpReward(25);
            daily5.setActive(true);
            missionRepository.save(daily5);

            Mission daily30 = new Mission();
            daily30.setCode("DAILY_30_MINUTES");
            daily30.setTitle("Estude por 30 Minutos");
            daily30.setDescription("Dedique 30 minutos ao estudo");
            daily30.setType("daily");
            daily30.setActionType("study_minutes");
            daily30.setRequiredValue(30);
            daily30.setXpReward(30);
            daily30.setActive(true);
            missionRepository.save(daily30);

            Mission streak = new Mission();
            streak.setCode("MAINTAIN_STREAK");
            streak.setTitle("Mantenha o Streak");
            streak.setDescription("Estude pelo menos uma vez hoje para manter seu streak");
            streak.setType("daily");
            streak.setActionType("streak");
            streak.setRequiredValue(1);
            streak.setXpReward(10);
            streak.setActive(true);
            missionRepository.save(streak);

            Mission weeklyMath = new Mission();
            weeklyMath.setCode("WEEKLY_MATH");
            weeklyMath.setTitle("Domine a Matemática");
            weeklyMath.setDescription("Complete uma lição completa de Matemática esta semana");
            weeklyMath.setType("weekly");
            weeklyMath.setActionType("complete_trail");
            weeklyMath.setRequiredValue(1);
            weeklyMath.setXpReward(100);
            weeklyMath.setActive(true);
            missionRepository.save(weeklyMath);
        }
    }

    /**
     * Classifica idempotentemente as contas existentes para separar os dados de
     * demonstração das contas criadas por testes automatizados.
     *
     * - {@code admin@revisalab.com} e {@code teste@revisalab.com} são os dados de
     *   demonstração (seed) e permanecem visíveis; o nome do usuário de teste é
     *   restaurado para o valor original caso um teste o tenha renomeado.
     * - As demais contas cujo nome segue os padrões usados pelos testes e2e são
     *   marcadas como {@code isTest=true} e ocultadas do ranking/painéis.
     */
    private void classifyExistingAccounts() {
        // As entidades retornadas por findAll() ficam desanexadas após o commit da
        // transação do repositório. Para que a classificação seja persistida, é
        // preciso salvar as MESMAS instâncias modificadas (o save faz merge), e não
        // reler o banco com um segundo findAll() — que devolveria cópias intactas.
        List<User> changedUsers = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase();

            boolean demo = email.equals("admin@revisalab.com") || email.equals("teste@revisalab.com");
            boolean modified = false;

            if (demo) {
                if (!Boolean.TRUE.equals(user.getIsDemo())) {
                    user.setIsDemo(true);
                    modified = true;
                }
                if (Boolean.TRUE.equals(user.getIsTest())) {
                    user.setIsTest(false);
                    modified = true;
                }
                // Desfaz renomeações de teste no usuário de demonstração.
                if (email.equals("teste@revisalab.com") && !"Bianca".equals(user.getName())) {
                    user.setName("Bianca");
                    modified = true;
                }
            } else if (!Boolean.TRUE.equals(user.getIsTest()) && looksLikeTestAccount(user.getName())) {
                user.setIsTest(true);
                modified = true;
            }

            if (modified) {
                changedUsers.add(user);
            }
        }
        if (!changedUsers.isEmpty()) {
            userRepository.saveAll(changedUsers);
        }
    }

    /**
     * Heurística para identificar contas criadas por testes automatizados a partir
     * do nome. Aplicada uma única vez sobre o banco existente; daqui em diante a
     * classificação é persistida no campo {@code isTest}.
     */
    private boolean looksLikeTestAccount(String name) {
        if (name == null || name.isBlank()) return false;
        String n = name.toLowerCase();
        return n.contains("teste")
                || n.contains("test ")
                || n.contains("smoke")
                || n.contains("probe")
                || n.contains("dedup")
                || n.contains("registrado")
                || n.contains(",")
                || n.matches(".*\\d{4,}.*");
    }
}
