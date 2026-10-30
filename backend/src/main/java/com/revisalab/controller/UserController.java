package com.revisalab.controller;

import com.revisalab.dto.request.ProfileUpdateRequest;
import com.revisalab.model.User;
import com.revisalab.repository.UserRepository;
import com.revisalab.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest request,
                                           Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName().trim());
        }
        if (request.getCurso() != null) {
            user.setCurso(request.getCurso().trim());
        }
        if (request.getSemestre() != null) {
            user.setSemestre(request.getSemestre().trim());
        }
        if (request.getInstituicao() != null) {
            user.setInstituicao(request.getInstituicao().trim());
        }

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("curso", user.getCurso());
        response.put("semestre", user.getSemestre());
        response.put("instituicao", user.getInstituicao());
        response.put("xp", user.getXp());
        response.put("level", user.getLevel());
        response.put("streak", user.getStreak());

        return ResponseEntity.ok(response);
    }
}
