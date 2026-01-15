package com.matheusdev.mindforge.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class LinkRepositoryRequest {
    @NotBlank(message = "A URL do repositório é obrigatória.")
    @URL(message = "A URL fornecida é inválida.")
    private String repoUrl;
}
