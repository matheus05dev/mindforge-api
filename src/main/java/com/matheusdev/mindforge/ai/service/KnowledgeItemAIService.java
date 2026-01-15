package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.service.DocumentService;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.knowledgeltem.mapper.KnowledgeItemMapper;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class KnowledgeItemAIService {

    private final KnowledgeItemRepository knowledgeItemRepository;
    private final DocumentService documentService;
    private final PromptCacheService promptCacheService;
    private final KnowledgeItemMapper knowledgeItemMapper;
    private final PromptBuilderService promptBuilderService;

    @Transactional
    public KnowledgeItemResponse modifyKnowledgeItemContent(Long itemId, String instruction) {
        KnowledgeItem item = knowledgeItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item de conhecimento não encontrado."));

        String prompt = promptBuilderService.buildContentModificationPrompt(item.getContent(), instruction);
        AIProviderRequest request = new AIProviderRequest(prompt);
        AIProviderResponse response = promptCacheService.executeRequest(request);

        if (response.getError() != null) {
            throw new BusinessException("Erro ao modificar conteúdo com IA: " + response.getError());
        }

        item.setContent(response.getContent());
        KnowledgeItem updatedItem = knowledgeItemRepository.save(item);
        return knowledgeItemMapper.toResponse(updatedItem);
    }

    @Transactional
    public KnowledgeItemResponse transcribeImageAndAppendToKnowledgeItem(Long documentId, Long itemId) throws IOException {
        KnowledgeItem item = knowledgeItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item de conhecimento não encontrado."));

        Document document = documentService.findDocumentById(documentId);
        byte[] imageBytes = documentService.getDocumentContent(document);

        String prompt = "Transcreva o texto contido nesta imagem. Se não houver texto, descreva a imagem.";
        AIProviderRequest request = new AIProviderRequest(prompt, imageBytes, document.getFileType());
        AIProviderResponse response = promptCacheService.executeRequest(request);

        if (response.getError() != null) {
            throw new BusinessException("Erro ao transcrever imagem com IA: " + response.getError());
        }

        String currentContent = item.getContent() == null ? "" : item.getContent();
        item.setContent(currentContent + "\n\n--- Transcrição da Imagem ---\n" + response.getContent());
        KnowledgeItem updatedItem = knowledgeItemRepository.save(item);
        return knowledgeItemMapper.toResponse(updatedItem);
    }
}
