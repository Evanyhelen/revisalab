package com.revisalab.controller;

import com.revisalab.dto.response.TrailDTO;
import com.revisalab.dto.response.TrailDetailDTO;
import com.revisalab.security.UserDetailsImpl;
import com.revisalab.service.StudyResourceService;
import com.revisalab.service.TrailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller de Trilhas.
 * Responsabilidades: receber requisições, extrair o usuário autenticado e delegar ao serviço.
 */
@RestController
@RequestMapping("/trails")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class TrailController {

    private final TrailService trailService;
    private final StudyResourceService studyResourceService;

    @GetMapping
    public ResponseEntity<List<TrailDTO>> getAllTrails(Authentication authentication) {
        return ResponseEntity.ok(trailService.getAllTrails(currentUserId(authentication)));
    }

    /**
     * Material de estudo externo (Wikipedia + YouTube) para uma trilha, por categoria/título.
     */
    @GetMapping("/study-resources")
    public ResponseEntity<Map<String, Object>> getStudyResources(@RequestParam String category,
                                                                 @RequestParam(required = false) String title) {
        return ResponseEntity.ok(studyResourceService.getStudyResources(category, title));
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Long>> getTrailCategories() {
        return ResponseEntity.ok(trailService.getCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrailDetailDTO> getTrailById(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseEntity.ok(trailService.getTrailById(id, currentUserId(authentication)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<TrailDTO>> getTrailsByCategory(@PathVariable String category,
                                                              Authentication authentication) {
        return ResponseEntity.ok(trailService.getTrailsByCategory(category, currentUserId(authentication)));
    }

    private Long currentUserId(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userDetails.getId();
    }
}
