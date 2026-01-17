package com.matheusdev.mindforge.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoteAIRequestDTO {

    @NotBlank(message = "A instrução não pode estar em branco.")
    @Schema(description = "A instrução para a IA sobre como processar a nota (ex: 'resuma este texto', 'traduza para inglês').", required = true)
    private String instruction;

    @Schema(description = "Provedor de IA a ser usado (ex: 'ollamaProvider', 'groqProvider'). Padrão: 'ollamaProvider'.")
    private String provider;
}
