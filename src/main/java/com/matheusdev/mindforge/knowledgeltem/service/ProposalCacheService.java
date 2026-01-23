package com.matheusdev.mindforge.knowledgeltem.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * In-memory cache for storing agent proposals.
 * REFACTORED: Now uses Caffeine Cache with automatic eviction policy (1 hour).
 */
@Service
@Slf4j
public class ProposalCacheService {

    private final Cache<String, KnowledgeAgentProposal> proposalCache;

    public ProposalCacheService() {
        this.proposalCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }

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
        return proposalCache.getIfPresent(proposalId);
    }

    /**
     * Remove a proposal after it's been applied or rejected
     */
    public void removeProposal(String proposalId) {
        proposalCache.invalidate(proposalId);
        log.info("Invalidated proposal {}", proposalId);
    }

    /**
     * Clear all proposals for a specific knowledge item
     * Note: Iterating over cache is expensive, but this operation is rare.
     */
    public void clearProposalsForKnowledge(Long knowledgeId) {
        proposalCache.asMap().entrySet().removeIf(entry -> entry.getValue().getKnowledgeId().equals(knowledgeId));
        log.info("Cleared all proposals for knowledge item {}", knowledgeId);
    }
}
