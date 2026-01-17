package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.ai.dto.PromptPair;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.service.DocumentService;
import com.matheusdev.mindforge.document.util.CustomMultipartFile;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.knowledgeltem.mapper.KnowledgeItemMapper;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class KnowledgeItemAIService {

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final DocumentService documentService;
    private final KnowledgeItemMapper knowledgeItemMapper;
    private final PromptBuilderService promptBuilderService;
    private final AIOrchestrationService aiOrchestrationService;

    @Transactional
    public KnowledgeItemResponse modifyKnowledgeItemContent(Long itemId, String instruction, String provider) {
        KnowledgeItem item = knowledgeItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item de conhecimento não encontrado."));

        PromptPair prompts = promptBuilderService.buildContentModificationPrompt(item.getContent(), instruction);
        ChatRequest chatRequest = new ChatRequest(prompts.userPrompt(), provider, null, prompts.systemPrompt());

        try {
            AIProviderResponse response = aiOrchestrationService.handleChatInteraction(chatRequest).get();

            if (response.getError() != null) {
                throw new BusinessException("Erro ao modificar conteúdo com IA: " + response.getError());
            }

            item.setContent(response.getContent());
            KnowledgeItem updatedItem = knowledgeItemRepository.save(item);
            return knowledgeItemMapper.toResponse(updatedItem);

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao processar a modificação de conteúdo com IA.", e);
        }
    }

    @Transactional
    public KnowledgeItemResponse transcribeImageAndAppendToKnowledgeItem(Long documentId, Long itemId, String provider) throws IOException {
        KnowledgeItem item = knowledgeItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item de conhecimento não encontrado."));

        Document document = documentService.findDocumentById(documentId);
        byte[] imageBytes = documentService.getDocumentContent(document);

        String prompt = "Transcreva o texto contido nesta imagem. Se não houver texto, descreva a imagem.";
        MultipartFile file = new CustomMultipartFile(imageBytes, document.getFileName(), document.getFileName(), document.getFileType());

        try {
            AIProviderResponse response = aiOrchestrationService.handleFileAnalysis(prompt, provider, file).get();

            if (response.getError() != null) {
                throw new BusinessException("Erro ao transcrever imagem com IA: " + response.getError());
            }

            String currentContent = item.getContent() == null ? "" : item.getContent();
            item.setContent(currentContent + "\n\n--- Transcrição da Imagem ---\n" + response.getContent());
            KnowledgeItem updatedItem = knowledgeItemRepository.save(item);
            return knowledgeItemMapper.toResponse(updatedItem);

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao processar a transcrição de imagem com IA.", e);
        }
    }
}
