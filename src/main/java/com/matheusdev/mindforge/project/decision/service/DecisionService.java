package com.matheusdev.mindforge.project.decision.service;

import com.matheusdev.mindforge.ai.service.AIOrchestrationService;
import com.matheusdev.mindforge.ai.provider.dto.AIProviderResponse;
import com.matheusdev.mindforge.ai.dto.ChatRequest;
import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.project.decision.dto.DecisionMetricsDTO;
import com.matheusdev.mindforge.project.decision.dto.DecisionRequest;
import com.matheusdev.mindforge.project.decision.dto.DecisionResponse;
import com.matheusdev.mindforge.project.decision.model.DecisionRecord;
import com.matheusdev.mindforge.project.decision.repository.DecisionRepository;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionService {

    private final DecisionRepository decisionRepository;
    private final ProjectRepository projectRepository;
    private final AIOrchestrationService aiOrchestrationService;
    private final ObjectMapper objectMapper;

    private static final String ADR_SYSTEM_PROMPT = "Você é um Arquiteto de Software Sênior especializado em Architecture Decision Records (ADR). "
            +
            "Sua tarefa é analisar o contexto fornecido e estruturar uma decisão arquitetural. " +
            "Responda EXCLUSIVAMENTE um JSON com os campos: title, context, decision, consequences (texto).";

    @Transactional(readOnly = true)
    public List<DecisionResponse> getDecisionsByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Projeto não encontrado");
        }
        return decisionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DecisionResponse createDecision(DecisionRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado"));

        DecisionRecord record = new DecisionRecord();
        record.setProject(project);
        record.setTitle(request.getTitle());
        record.setStatus(request.getStatus());
        record.setContext(request.getContext());
        record.setDecision(request.getDecision());
        record.setConsequences(request.getConsequences());
        record.setTags(request.getTags());
        record.setAuthor(request.getAuthor());

        return toResponse(decisionRepository.save(record));
    }

    @Transactional
    public DecisionResponse updateDecision(Long id, DecisionRequest request) {
        DecisionRecord record = decisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Decisão não encontrada"));

        record.setTitle(request.getTitle());
        record.setStatus(request.getStatus());
        record.setContext(request.getContext());
        record.setDecision(request.getDecision());
        record.setConsequences(request.getConsequences());
        record.setTags(request.getTags());
        if (request.getAuthor() != null) {
            record.setAuthor(request.getAuthor());
        }

        return toResponse(decisionRepository.save(record));
    }

    public DecisionResponse proposeDecisionFromContext(Long projectId, String contextText) {
        try {
            ChatRequest chatRequest = new ChatRequest(
                    null,
                    null,
                    "Contexto da decisão: " + contextText,
                    "groqProvider",
                    "llama3-70b-8192",
                    ADR_SYSTEM_PROMPT);

            AIProviderResponse response = aiOrchestrationService.handleChatInteraction(chatRequest).get();

            if (response.getError() != null) {
                throw new BusinessException("Erro na IA: " + response.getError());
            }

            // Parse JSON response
            // Assuming the AI follows the JSON instruction strictly.
            // In a real scenario, we might need a parser resilience layer.
            Map<String, String> adrData = objectMapper.readValue(response.getContent(), Map.class);

            DecisionResponse proposal = new DecisionResponse();
            proposal.setProjectId(projectId);
            proposal.setTitle(adrData.getOrDefault("title", "Nova Decisão"));
            proposal.setContext(adrData.getOrDefault("context", "Contexto identificado..."));
            proposal.setDecision(adrData.getOrDefault("decision", "Decisão sugerida..."));
            proposal.setConsequences(adrData.getOrDefault("consequences", "Consequências..."));

            return proposal;

        } catch (Exception e) {
            log.error("Erro ao gerar proposta de decisão", e);
            throw new BusinessException("Falha ao gerar proposta com IA", e);
        }
    }

    public DecisionMetricsDTO getProjectMetrics(Long projectId) {
        List<DecisionRecord> decisions = decisionRepository.findByProjectId(projectId);

        long total = decisions.size();
        if (total == 0) {
            return DecisionMetricsDTO.builder()
                    .totalDecisions(0)
                    .acceptanceRate(0.0)
                    .volatilityScore(0.0)
                    .build();
        }

        long accepted = decisions.stream()
                .filter(d -> d.getStatus() == com.matheusdev.mindforge.project.decision.model.DecisionStatus.ACCEPTED)
                .count();
        long proposed = decisions.stream()
                .filter(d -> d.getStatus() == com.matheusdev.mindforge.project.decision.model.DecisionStatus.PROPOSED)
                .count();
        long rejected = decisions.stream()
                .filter(d -> d.getStatus() == com.matheusdev.mindforge.project.decision.model.DecisionStatus.REJECTED)
                .count();
        long deprecated = decisions.stream()
                .filter(d -> d.getStatus() == com.matheusdev.mindforge.project.decision.model.DecisionStatus.DEPRECATED)
                .count();

        double acceptanceRate = (double) accepted / total;
        // Volatilidade: decisões que foram rejeitadas ou que se tornaram obsoletas
        // (mudança de plano)
        double volatility = (double) (rejected + deprecated) / total;

        return DecisionMetricsDTO.builder()
                .totalDecisions(total)
                .acceptedCount(accepted)
                .proposedCount(proposed)
                .rejectedCount(rejected)
                .deprecatedCount(deprecated)
                .acceptanceRate(acceptanceRate)
                .volatilityScore(volatility)
                .build();
    }

    private DecisionResponse toResponse(DecisionRecord record) {
        DecisionResponse response = new DecisionResponse();
        response.setId(record.getId());
        response.setProjectId(record.getProject().getId());
        response.setTitle(record.getTitle());
        response.setStatus(record.getStatus());
        response.setContext(record.getContext());
        response.setDecision(record.getDecision());
        response.setConsequences(record.getConsequences());
        response.setTags(record.getTags());
        response.setAuthor(record.getAuthor());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        return response;
    }
}
