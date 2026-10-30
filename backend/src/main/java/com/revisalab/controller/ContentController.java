package com.revisalab.controller;

import com.revisalab.model.Trail;
import com.revisalab.model.Module;
import com.revisalab.model.Exercise;
import com.revisalab.model.User;
import com.revisalab.repository.TrailRepository;
import com.revisalab.repository.UserRepository;
import com.revisalab.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/content")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class ContentController {
    
    @Autowired
    private TrailRepository trailRepository;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/trails/mock")
    public ResponseEntity<?> getMockTrails() {
        // Trilhas mockadas de exemplo
        List<Map<String, Object>> mockTrails = List.of(
            createMockTrail(1001, "Matemática Básica - Exemplo", "Conceitos fundamentais de matemática (exemplo)", "Matemática", 2, "calculate", "#3f51b5", 4, 40, 10),
            createMockTrail(1002, "Português Essencial - Exemplo", "Gramática e interpretação (exemplo)", "Português", 1, "edit_note", "#f44336", 3, 30, 8)
        );
        
        return ResponseEntity.ok(mockTrails);
    }
    
    private Map<String, Object> createMockTrail(int id, String title, String description, String category, 
                                                int difficulty, String icon, String color, 
                                                int totalModules, int totalExercises, int estimatedHours) {
        Map<String, Object> trail = new HashMap<>();
        trail.put("id", id);
        trail.put("title", title);
        trail.put("description", description);
        trail.put("category", category);
        trail.put("difficulty", difficulty);
        trail.put("icon", icon);
        trail.put("color", color);
        trail.put("totalModules", totalModules);
        trail.put("totalExercises", totalExercises);
        trail.put("estimatedHours", estimatedHours);
        trail.put("isMock", true);
        trail.put("progress", 0.0);
        trail.put("completedExercises", 0);
        return trail;
    }
    
    @GetMapping("/exercises/mock")
    public ResponseEntity<?> getMockExercises() {
        // Exercícios mockados de exemplo
        List<Map<String, Object>> mockExercises = List.of(
            createMockExercise(2001, "Qual é a derivada de f(x) = x²? (Exemplo)", "multiple-choice", 
                List.of("2x", "x", "2", "x²"), "A", 
                "A derivada de x² é 2x (exemplo de explicação)", 2, "Matemática", 10),
            createMockExercise(2002, "Qual é a capital do Brasil? (Exemplo)", "multiple-choice",
                List.of("Rio de Janeiro", "São Paulo", "Brasília", "Salvador"), "C",
                "Brasília é a capital federal do Brasil (exemplo)", 1, "Geografia", 10)
        );
        
        return ResponseEntity.ok(mockExercises);
    }
    
    private Map<String, Object> createMockExercise(int id, String question, String type,
                                                    List<String> options, String correctAnswer,
                                                    String explanation, int difficulty,
                                                    String category, int xpReward) {
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("id", id);
        exercise.put("question", question);
        exercise.put("type", type);
        
        List<Map<String, String>> optionsList = new java.util.ArrayList<>();
        char optionChar = 'A';
        for (String option : options) {
            Map<String, String> opt = new HashMap<>();
            opt.put("id", String.valueOf(optionChar));
            opt.put("text", option);
            optionsList.add(opt);
            optionChar++;
        }
        exercise.put("options", optionsList);
        exercise.put("correctAnswer", correctAnswer);
        exercise.put("explanation", explanation);
        exercise.put("difficulty", difficulty);
        exercise.put("category", category);
        exercise.put("xpReward", xpReward);
        exercise.put("isMock", true);
        
        return exercise;
    }
    
    @PostMapping("/trails")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTrail(@RequestBody Map<String, Object> trailData,
                                        Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Trail trail = new Trail();
        trail.setTitle((String) trailData.get("title"));
        trail.setDescription((String) trailData.get("description"));
        trail.setCategory((String) trailData.get("category"));
        // `difficulty` é obrigatório na tabela; assume 1 quando ausente para evitar NPE/500.
        trail.setDifficulty(trailData.get("difficulty") instanceof Number n ? n.intValue() : 1);
        trail.setIcon((String) trailData.get("icon"));
        trail.setColor((String) trailData.get("color"));
        trail.setTotalModules((Integer) trailData.getOrDefault("totalModules", 0));
        trail.setTotalExercises((Integer) trailData.getOrDefault("totalExercises", 0));
        trail.setEstimatedHours((Integer) trailData.getOrDefault("estimatedHours", 0));
        
        Trail savedTrail = trailRepository.save(trail);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedTrail.getId());
        response.put("title", savedTrail.getTitle());
        response.put("description", savedTrail.getDescription());
        response.put("category", savedTrail.getCategory());
        response.put("difficulty", savedTrail.getDifficulty());
        response.put("icon", savedTrail.getIcon());
        response.put("color", savedTrail.getColor());
        response.put("totalModules", savedTrail.getTotalModules());
        response.put("totalExercises", savedTrail.getTotalExercises());
        response.put("estimatedHours", savedTrail.getEstimatedHours());
        response.put("createdAt", savedTrail.getCreatedAt());
        response.put("isMock", false);
        response.put("createdBy", user.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/combined/trails")
    public ResponseEntity<?> getCombinedTrails(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Buscar trilhas reais do banco
        List<Trail> realTrails = trailRepository.findAll();
        
        // Converter trilhas reais para o formato de resposta
        List<Map<String, Object>> realTrailsResponse = realTrails.stream()
                .map(trail -> {
                    Map<String, Object> trailData = new HashMap<>();
                    trailData.put("id", trail.getId());
                    trailData.put("title", trail.getTitle());
                    trailData.put("description", trail.getDescription());
                    trailData.put("category", trail.getCategory());
                    trailData.put("difficulty", trail.getDifficulty());
                    trailData.put("icon", trail.getIcon());
                    trailData.put("color", trail.getColor());
                    trailData.put("totalModules", trail.getTotalModules());
                    trailData.put("totalExercises", trail.getTotalExercises());
                    trailData.put("estimatedHours", trail.getEstimatedHours());
                    trailData.put("createdAt", trail.getCreatedAt());
                    trailData.put("isMock", false);
                    trailData.put("progress", 0.0); // Será calculado com progresso real
                    trailData.put("completedExercises", 0);
                    return trailData;
                })
                .collect(Collectors.toList());
        
        // Adicionar trilhas mockadas
        List<Map<String, Object>> mockTrails = (List<Map<String, Object>>) getMockTrails().getBody();
        
        // Combinar: Mockados primeiro, depois reais (List.of é imutável, então copiamos).
        List<Map<String, Object>> combinedTrails = new ArrayList<>(mockTrails);
        combinedTrails.addAll(realTrailsResponse);
        
        return ResponseEntity.ok(combinedTrails);
    }
    
    @GetMapping("/user-content")
    public ResponseEntity<?> getUserCreatedContent(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Conteúdo criado pelo usuário (simulação)
        Map<String, Object> userContent = new HashMap<>();
        userContent.put("userTrails", 2); // Trilhas criadas pelo usuário
        userContent.put("userExercises", 15); // Exercícios criados
        userContent.put("userAchievements", 0); // Conquistas criadas
        userContent.put("lastCreated", LocalDateTime.now().minusDays(1).toString());
        
        return ResponseEntity.ok(userContent);
    }
    
    @GetMapping("/categories/combined")
    public ResponseEntity<?> getCombinedCategories() {
        // Categorias mockadas + reais
        Map<String, Long> categories = new HashMap<>();
        
        // Categorias mockadas
        categories.put("Matemática", 2L);
        categories.put("Português", 1L);
        categories.put("Geografia", 1L);
        
        // Adicionar categorias reais
        List<Trail> realTrails = trailRepository.findAll();
        realTrails.forEach(trail -> {
            String category = trail.getCategory();
            categories.put(category, categories.getOrDefault(category, 0L) + 1);
        });
        
        return ResponseEntity.ok(categories);
    }
}