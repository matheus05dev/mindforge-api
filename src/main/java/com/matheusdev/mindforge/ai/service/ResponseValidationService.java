package com.matheusdev.mindforge.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusdev.mindforge.ai.service.model.AuditedAnswer;
import com.matheusdev.mindforge.ai.service.model.Evidence;
import com.matheusdev.mindforge.ai.service.model.EvidenceRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResponseValidationService {

    private final ObjectMapper objectMapper;

    public Optional<AuditedAnswer> validateAndParse(String rawResponse, List<Evidence> evidences) {
        try {
            String cleanedJson = cleanJson(rawResponse);
            AuditedAnswer auditedAnswer = objectMapper.readValue(cleanedJson, AuditedAnswer.class);
            validateAuditedAnswer(auditedAnswer, evidences);
            log.info("Resposta RAG validada e parseada com sucesso.");
            return Optional.of(auditedAnswer);
        } catch (Exception e) {
            log.error("Erro ao validar ou parsear resposta RAG: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private String cleanJson(String rawResponse) {
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private void validateAuditedAnswer(AuditedAnswer auditedAnswer, List<Evidence> evidences) {
        if (auditedAnswer.references() == null || auditedAnswer.references().isEmpty()) {
            log.warn("Resposta auditada não contém referências.");
            return;
        }

        for (EvidenceRef ref : auditedAnswer.references()) {
            if (ref.evidenceId() <= 0 || ref.evidenceId() > evidences.size()) {
                throw new IllegalArgumentException("ID de evidência inválido: " + ref.evidenceId());
            }
        }
    }
}
