package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de resposta de autenticação (contém o token JWT e os dados do usuário).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type;
    private Long id;
    private String name;
    private String email;
    private List<String> roles;
    private Integer xp;
    private Integer level;
    private Integer streak;
    private String curso;
    private String semestre;
    private String instituicao;

    @Builder.Default
    private String tokenType = "Bearer";

    // Construtor mantido para compatibilidade com código existente.
    public JwtResponse(String token, Long id, String name, String email,
                       Integer xp, Integer level, Integer streak) {
        this.token = token;
        this.type = "Bearer";
        this.id = id;
        this.name = name;
        this.email = email;
        this.xp = xp;
        this.level = level;
        this.streak = streak;
        this.tokenType = "Bearer";
    }
}
