package com.matheusdev.mindforge.note.service;

import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import com.matheusdev.mindforge.ai.service.PromptBuilderService;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.note.dto.NoteAIRequestDTO;
import com.matheusdev.mindforge.note.dto.NoteResponseDTO;
import com.matheusdev.mindforge.note.mapper.NoteMapper;
import com.matheusdev.mindforge.note.model.Note;
import com.matheusdev.mindforge.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class NoteAIService {

    private final NoteRepository noteRepository;
    private final PromptBuilderService promptBuilderService;
    private final AIOrchestrationService aiOrchestrationService;
    private final NoteMapper noteMapper;

    @Transactional
    public NoteResponseDTO processNoteWithAI(Long noteId, NoteAIRequestDTO requestDTO) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Nota não encontrada"));

        PromptPair prompts = promptBuilderService.buildContentModificationPrompt(note.getContent(), requestDTO.getInstruction());

        try {
            ChatRequest chatRequest = new ChatRequest(prompts.userPrompt(), requestDTO.getProvider(), null, prompts.systemPrompt());
            AIProviderResponse aiResponse = aiOrchestrationService.handleChatInteraction(chatRequest).get();

            if (aiResponse.getError() != null) {
                throw new BusinessException("Erro no Serviço de IA: " + aiResponse.getError());
            }

            note.setContent(aiResponse.getContent());
            Note updatedNote = noteRepository.save(note);

            return noteMapper.toDTO(updatedNote);
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao processar a nota com IA.", e);
        }
    }
}
