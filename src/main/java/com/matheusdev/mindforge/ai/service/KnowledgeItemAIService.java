package com.matheusdev.mindforge.ai.service;

import com.matheusdev.mindforge.ai.provider.dto.AIProviderRequest;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.document.model.Document;
import com.matheusdev.mindforge.document.repository.DocumentRepository;
import com.matheusdev.mindforge.document.service.FileStorageService;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse;
import com.matheusdev.mindforge.knowledgeltem.mapper.KnowledgeItemMapper;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;

@Service
@RequiredArgsConstructor
public class KnowledgeItemAIService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeItemMapper knowledgeItemMapper;
    private final PromptBuilderService promptBuilderService;
    private final PromptCacheService promptCacheService;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public KnowledgeItemResponse modifyKnowledgeItemContent(Long itemId, String instruction) {
        KnowledgeItem item = knowledgeBaseService.getItemById(itemId);
        String prompt = promptBuilderService.buildContentModificationPrompt(item.getContent(), instruction);

        AIProviderResponse aiResponse = promptCacheService.executeRequest(new AIProviderRequest(prompt));

        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }

        item.setContent(aiResponse.getContent());
        KnowledgeItem updatedItem = knowledgeBaseService.updateItem(itemId, item);
        return knowledgeItemMapper.toResponse(updatedItem);
    }

    @Transactional
    public KnowledgeItemResponse transcribeImageAndAppendToKnowledgeItem(Long documentId, Long itemId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado com o id: " + documentId));

        if (document.getFileType() == null || !document.getFileType().startsWith("image")) {
            throw new BusinessException("O documento fornecido não é uma imagem.");
        }

        Resource resource = fileStorageService.loadFileAsResource(document.getFileName());
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        String prompt = "Transcreva o texto contido nesta imagem.";
        AIProviderRequest request = new AIProviderRequest(prompt, fileContent, document.getFileType());

        AIProviderResponse aiResponse = promptCacheService.executeRequest(request);

        if (aiResponse.getError() != null) {
            throw new BusinessException("Erro no serviço de IA: " + aiResponse.getError());
        }

        String transcribedText = aiResponse.getContent();

        KnowledgeItem item = knowledgeBaseService.getItemById(itemId);
        String currentContent = item.getContent() == null ? "" : item.getContent();

        item.setContent(currentContent + "\n\n--- TEXTO TRANSCRITO DA IMAGEM " + document.getFileName() + " ---\n" + transcribedText);

        KnowledgeItem updatedItem = knowledgeBaseService.updateItem(itemId, item);

        return knowledgeItemMapper.toResponse(updatedItem);
    }
}
