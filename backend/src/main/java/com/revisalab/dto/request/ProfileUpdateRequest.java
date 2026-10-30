package com.revisalab.dto.request;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String name;
    private String curso;
    private String semestre;
    private String instituicao;
}
