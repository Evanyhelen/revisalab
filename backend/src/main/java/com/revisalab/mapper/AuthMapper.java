package com.revisalab.mapper;

import com.revisalab.dto.response.JwtResponse;
import com.revisalab.model.User;
import com.revisalab.security.UserDetailsImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper para conversões relacionadas à autenticação e autorização.
 */
@Component
public class AuthMapper {

    /**
     * Converte User para JwtResponse com token JWT.
     */
    public JwtResponse toJwtResponse(User user, String jwtToken) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return JwtResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roles(roles)
                .xp(user.getXp())
                .level(user.getLevel())
                .streak(user.getStreak())
                .curso(user.getCurso())
                .semestre(user.getSemestre())
                .instituicao(user.getInstituicao())
                .tokenType("Bearer")
                .build();
    }

    /**
     * Converte UserDetailsImpl para JwtResponse com token JWT.
     */
    public JwtResponse toJwtResponse(UserDetailsImpl userDetails, String jwtToken) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return JwtResponse.builder()
                .token(jwtToken)
                .type("Bearer")
                .id(userDetails.getId())
                .name(userDetails.getName())
                .email(userDetails.getEmail())
                .roles(roles)
                .xp(userDetails.getXp())
                .level(userDetails.getLevel())
                .streak(userDetails.getStreak())
                .tokenType("Bearer")
                .build();
    }
}
