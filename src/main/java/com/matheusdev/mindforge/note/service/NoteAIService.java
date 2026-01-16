package com.matheusdev.mindforge.note.service;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.PromptBuilderService;
import com.matheusdev.mindforge.ai.service.PromptCacheService;
import com.matheusdev.mindforge.note.dto.NoteAIRequestDTO;
import com.matheusdev.mindforge.note.dto.NoteResponseDTO;
import com.matheusdev.mindforge.note.mapper.NoteMapper;
import com.matheusdev.mindforge.note.model.Note;
import com.matheusdev.mindforge.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteAIService {

    private final NoteRepository noteRepository;
    private final PromptBuilderService promptBuilderService;
    private final PromptCacheService promptCacheService;
    private final NoteMapper noteMapper;

    @Transactional
    public NoteResponseDTO processNoteWithAI(Long noteId, NoteAIRequestDTO requestDTO) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota não encontrada"));

        String prompt = promptBuilderService.buildContentModificationPrompt(note.getContent(), requestDTO.getInstruction());
        
        // Usando uma mensagem de sistema padrão e modelo por enquanto, similar à análise genérica
        String systemMessage = "Você é um assistente de IA útil especializado em edição e melhoria de texto.";
        String model = "gemini-pro"; // Padronizando para um modelo conhecido ou poderia ser dinâmico
        
        AIProviderRequest aiRequest = new AIProviderRequest(prompt, systemMessage, model, null);
        AIProviderResponse aiResponse = promptCacheService.executeRequest(aiRequest);

        if (aiResponse.getError() != null) {
            throw new RuntimeException("Erro no Serviço de IA: " + aiResponse.getError());
        }

        // Atualiza o conteúdo da nota com a resposta da IA
        note.setContent(aiResponse.getContent());
        Note updatedNote = noteRepository.save(note);

        return noteMapper.toDTO(updatedNote);
    }
}
