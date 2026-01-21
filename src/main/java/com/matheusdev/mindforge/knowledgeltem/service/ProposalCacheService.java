package com.matheusdev.mindforge.knowledgeltem.service;

import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for storing agent proposals temporarily
 * until user approves or rejects them.
 */
@Service
@Slf4j
public class ProposalCacheService {

    private final Map<String, KnowledgeAgentProposal> proposalCache = new ConcurrentHashMap<>();

    /**
     * Store a proposal and return its ID
     */
    public String storeProposal(KnowledgeAgentProposal proposal) {
        String proposalId = UUID.randomUUID().toString();
        proposal.setProposalId(proposalId);
        proposalCache.put(proposalId, proposal);
        log.info("Stored proposal {} for knowledge item {}", proposalId, proposal.getKnowledgeId());
        return proposalId;
    }

    /**
     * Retrieve a proposal by ID
     */
    public KnowledgeAgentProposal getProposal(String proposalId) {
        return proposalCache.get(proposalId);
    }

    /**
     * Remove a proposal after it's been applied or rejected
     */
    public void removeProposal(String proposalId) {
        KnowledgeAgentProposal removed = proposalCache.remove(proposalId);
        if (removed != null) {
            log.info("Removed proposal {} for knowledge item {}", proposalId, removed.getKnowledgeId());
        }
    }

    /**
     * Clear all proposals for a specific knowledge item
     */
    public void clearProposalsForKnowledge(Long knowledgeId) {
        proposalCache.entrySet().removeIf(entry -> entry.getValue().getKnowledgeId().equals(knowledgeId));
        log.info("Cleared all proposals for knowledge item {}", knowledgeId);
    }
}
