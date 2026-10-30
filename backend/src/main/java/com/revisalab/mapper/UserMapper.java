package com.revisalab.mapper;

import com.revisalab.model.User;
import com.revisalab.dto.request.SignupRequest;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversões relacionadas a usuários
 */
@Component
public class UserMapper {

    /**
     * Converte SignupRequest para entidade User
     */
    public User toEntity(SignupRequest signupRequest) {
        return User.builder()
                .name(signupRequest.getName())
                .email(signupRequest.getEmail())
                .password(signupRequest.getPassword()) // Será criptografada no serviço
                .curso(signupRequest.getCurso())
                .semestre(signupRequest.getSemestre())
                .instituicao(signupRequest.getInstituicao())
                .xp(0)
                .level(1)
                .streak(0)
                .build();
    }
}
