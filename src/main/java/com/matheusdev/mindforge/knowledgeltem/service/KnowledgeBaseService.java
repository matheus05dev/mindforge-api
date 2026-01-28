package com.matheusdev.mindforge.knowledgeltem.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeVersion;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository;
import com.matheusdev.mindforge.core.tenant.context.TenantContext;
import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@org.springframework.transaction.annotation.Transactional
public class KnowledgeBaseService {

    private final KnowledgeItemRepository repository;
    private final WorkspaceRepository workspaceRepository;
    private final com.matheusdev.mindforge.knowledgeltem.mapper.KnowledgeItemMapper mapper;
    private final com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeVersionRepository versionRepository;

    public List<KnowledgeItem> getAllKnowledgeItems() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }
        return repository.findByTenantId(tenantId);
    }

    public KnowledgeItem getItemById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Item de conhecimento não encontrado com o id: " + id));
    }

    public KnowledgeItem createItem(KnowledgeItem item, String workspaceId) {
        Workspace workspace = resolveWorkspace(workspaceId);
        item.setWorkspace(workspace);
        // Tenant listener will set tenant from context on save
        return repository.save(item);
    }

    private Workspace resolveWorkspace(String identifier) {
        Long tenantId = TenantContext.getTenantId();

        // 1. If no identifier provided, try default "Geral"
        if (identifier == null || identifier.trim().isEmpty()) {
            return findWorkspaceInTenant("Geral", tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Workspace ID not provided and default 'Geral' not found for this tenant."));
        }

        // 2. Try to verify if it is an ID first (standard flow)
        try {
            Long id = Long.parseLong(identifier);
            return workspaceRepository.findByIdAndTenantId(id, tenantId)
                    .orElseGet(() -> {
                        // FALLBACK: If ID was "1" (common frontend default) but not found in this
                        // tenant,
                        // try to find the "Geral" workspace for this tenant.
                        // This handles the case where frontend hardcodes ID=1 but DB generated a
                        // different ID.
                        if (id == 1L) {
                            return findWorkspaceInTenant("Geral", tenantId)
                                    .orElseThrow(() -> new ResourceNotFoundException(
                                            "Workspace not found with ID: " + id
                                                    + " and fallback 'Geral' also not found."));
                        }
                        throw new ResourceNotFoundException("Workspace not found with ID: " + id);
                    });
        } catch (NumberFormatException e) {
            // 3. If not an ID, try as Name
            return findWorkspaceInTenant(identifier, tenantId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Workspace not found with identifier: " + identifier));
        }
    }

    private java.util.Optional<Workspace> findWorkspaceInTenant(String name, Long tenantId) {
        return workspaceRepository.findByNameIgnoreCaseAndTenantId(name, tenantId);
    }

    public KnowledgeItem updateItem(Long id, KnowledgeItem item) {
        KnowledgeItem existingItem = getItemById(id);
        existingItem.setTitle(item.getTitle());
        existingItem.setContent(item.getContent());
        existingItem.setTags(item.getTags());
        return repository.save(existingItem);
    }

    public void deleteItem(Long id) {
        // getItemById already checks tenant
        KnowledgeItem item = getItemById(id);
        repository.delete(item);
    }

    public List<KnowledgeItem> searchByTag(String tag) {
        // Assuming searchByTag needs to be tenant aware too.
        // We probably need to update repository to filter searchByTag by tenant.
        // For now, let's filter in memory if repo doesn't support it, but better to
        // update repo.
        // But to be safe, let's assume getAll and filter.
        // Wait, repository.findByTag probably leaks!
        // This method is DANGEROUS if not fixed.
        // I will use findAll and filter relative to tenant for safety now.
        Long tenantId = TenantContext.getTenantId();
        return repository.findByTenantId(tenantId).stream()
                .filter(item -> item.getTags() != null && item.getTags().contains(tag))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Apply approved agent proposal to knowledge item
     */
    public com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse applyProposal(
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal proposal,
            com.matheusdev.mindforge.knowledgeltem.dto.ApprovalRequest approval) {

        // 1. Get the knowledge item
        KnowledgeItem item = getItemById(proposal.getKnowledgeId());
        String currentContent = item.getContent() != null ? item.getContent() : "";

        // 2. Save version BEFORE applying changes (for rollback)
        String changeSummary = approval.isApproveAll()
                ? "Applied all changes from agent proposal"
                : String.format("Applied %d selected changes from agent proposal",
                        approval.getApprovedChangeIndices().size());

        saveVersion(
                proposal.getKnowledgeId(),
                proposal.getProposalId(),
                KnowledgeVersion.ChangeType.AGENT_PROPOSAL,
                changeSummary);

        // 3. Determine which changes to apply
        List<com.matheusdev.mindforge.knowledgeltem.dto.ContentChange> changesToApply = new java.util.ArrayList<>();

        if (approval.isApproveAll()) {
            // Add all, respecting manual edits
            for (int i = 0; i < proposal.getChanges().size(); i++) {
                com.matheusdev.mindforge.knowledgeltem.dto.ContentChange change = proposal.getChanges().get(i);

                // Check if this change has a manual edit
                if (approval.getModifiedContent() != null && approval.getModifiedContent().containsKey(i)) {
                    // Create a copy or modify
                    com.matheusdev.mindforge.knowledgeltem.dto.ContentChange modifiedChange = new com.matheusdev.mindforge.knowledgeltem.dto.ContentChange();
                    modifiedChange.setType(change.getType());
                    modifiedChange.setStartLine(change.getStartLine());
                    modifiedChange.setEndLine(change.getEndLine());
                    modifiedChange.setOriginalContent(change.getOriginalContent());
                    modifiedChange.setReason(change.getReason());
                    // Apply manual edit
                    modifiedChange.setProposedContent(approval.getModifiedContent().get(i));

                    changesToApply.add(modifiedChange);
                } else {
                    changesToApply.add(change);
                }
            }
        } else {
            // Apply only approved changes by index
            if (approval.getApprovedChangeIndices() != null) {
                for (Integer index : approval.getApprovedChangeIndices()) {
                    if (index >= 0 && index < proposal.getChanges().size()) {
                        com.matheusdev.mindforge.knowledgeltem.dto.ContentChange change = proposal.getChanges()
                                .get(index);

                        // Check for manual edit
                        if (approval.getModifiedContent() != null && approval.getModifiedContent().containsKey(index)) {
                            com.matheusdev.mindforge.knowledgeltem.dto.ContentChange modifiedChange = new com.matheusdev.mindforge.knowledgeltem.dto.ContentChange();
                            modifiedChange.setType(change.getType());
                            modifiedChange.setStartLine(change.getStartLine());
                            modifiedChange.setEndLine(change.getEndLine());
                            modifiedChange.setOriginalContent(change.getOriginalContent());
                            modifiedChange.setReason(change.getReason());
                            // Apply manual edit
                            modifiedChange.setProposedContent(approval.getModifiedContent().get(index));

                            changesToApply.add(modifiedChange);
                        } else {
                            changesToApply.add(change);
                        }
                    }
                }
            }
        }

        // 4. Apply changes to content
        String newContent = applyChangesToContent(currentContent, changesToApply);

        // 5. Update the knowledge item
        item.setContent(newContent);
        KnowledgeItem updated = repository.save(item);

        // 6. Return response
        return mapper.toResponse(updated);
    }

    /**
     * Apply a list of content changes to the original content
     */
    private String applyChangesToContent(
            String originalContent,
            List<com.matheusdev.mindforge.knowledgeltem.dto.ContentChange> changes) {

        String result = originalContent;

        // Sort changes by line number (descending) to avoid offset issues
        List<com.matheusdev.mindforge.knowledgeltem.dto.ContentChange> sortedChanges = new java.util.ArrayList<>(
                changes);
        sortedChanges.sort((a, b) -> Integer.compare(b.getStartLine(), a.getStartLine()));

        for (com.matheusdev.mindforge.knowledgeltem.dto.ContentChange change : sortedChanges) {
            result = applySingleChange(result, change);
        }

        return result;
    }

    /**
     * Apply a single change to content
     */
    private String applySingleChange(
            String content,
            com.matheusdev.mindforge.knowledgeltem.dto.ContentChange change) {

        switch (change.getType()) {
            case ADD:
                if (change.getStartLine() == 0) {
                    return change.getProposedContent() + "\n" + content;
                } else {
                    String[] lines = content.split("\n", -1);
                    if (change.getStartLine() <= lines.length) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < change.getStartLine(); i++) {
                            sb.append(lines[i]).append("\n");
                        }
                        sb.append(change.getProposedContent()).append("\n");
                        for (int i = change.getStartLine(); i < lines.length; i++) {
                            sb.append(lines[i]);
                            if (i < lines.length - 1)
                                sb.append("\n");
                        }
                        return sb.toString();
                    }
                }
                return content + "\n" + change.getProposedContent();

            case REMOVE:
                String[] linesR = content.split("\n", -1);
                StringBuilder sbR = new StringBuilder();
                for (int i = 0; i < linesR.length; i++) {
                    if (i < change.getStartLine() || i > change.getEndLine()) {
                        sbR.append(linesR[i]);
                        if (i < linesR.length - 1)
                            sbR.append("\n");
                    }
                }
                return sbR.toString();

            case REPLACE:
                if (change.getOriginalContent() != null && !change.getOriginalContent().isEmpty()) {
                    return content.replace(change.getOriginalContent(), change.getProposedContent());
                } else {
                    String[] contentLines = content.split("\n", -1);
                    StringBuilder resultB = new StringBuilder();
                    for (int i = 0; i < contentLines.length; i++) {
                        if (i == change.getStartLine()) {
                            resultB.append(change.getProposedContent());
                            if (i < contentLines.length - 1)
                                resultB.append("\n");
                            i = change.getEndLine();
                        } else {
                            resultB.append(contentLines[i]);
                            if (i < contentLines.length - 1)
                                resultB.append("\n");
                        }
                    }
                    return resultB.toString();
                }

            default:
                return content;
        }
    }

    // ==================== VERSION MANAGEMENT ====================

    /**
     * Save a version snapshot before making changes
     */
    public void saveVersion(Long knowledgeItemId, String proposalId,
            KnowledgeVersion.ChangeType changeType,
            String changeSummary) {
        KnowledgeItem item = getItemById(knowledgeItemId);

        KnowledgeVersion version = KnowledgeVersion
                .builder()
                .knowledgeItemId(knowledgeItemId)
                .title(item.getTitle())
                .content(item.getContent())
                .changeType(changeType)
                .proposalId(proposalId)
                .changeSummary(changeSummary)
                .build();

        versionRepository.save(version);

        // Cleanup old versions if needed (keep last 10)
        cleanupOldVersions(knowledgeItemId);
    }

    /**
     * Get version history for a knowledge item
     */
    public List<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse> getVersionHistory(
            Long knowledgeItemId) {
        // Enforce tenant access via getItemById check
        getItemById(knowledgeItemId);

        List<KnowledgeVersion> versions = versionRepository
                .findByKnowledgeItemIdOrderByCreatedAtDesc(knowledgeItemId);

        return versions.stream()
                .map(v -> com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse.fromEntity(v, false))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get a specific version with full content
     */
    public com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse getVersion(Long versionId) {
        KnowledgeVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + versionId));

        // Security check: ensure item belongs to tenant
        getItemById(version.getKnowledgeItemId());

        return com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse.fromEntity(version, true);
    }

    /**
     * Rollback to a specific version
     */
    public com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeItemResponse rollbackToVersion(Long knowledgeItemId,
            Long versionId) {
        // Get the version to rollback to
        KnowledgeVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + versionId));

        if (!version.getKnowledgeItemId().equals(knowledgeItemId)) {
            throw new IllegalArgumentException("Version does not belong to this knowledge item");
        }

        // Security check: ensure item belongs to tenant
        getItemById(knowledgeItemId);

        // Save current state as a version before rollback
        saveVersion(knowledgeItemId, null,
                KnowledgeVersion.ChangeType.ROLLBACK,
                "Before rollback to version " + versionId);

        // Apply the rollback
        KnowledgeItem item = getItemById(knowledgeItemId);
        item.setTitle(version.getTitle());
        item.setContent(version.getContent());
        KnowledgeItem updated = repository.save(item);

        return mapper.toResponse(updated);
    }

    /**
     * Cleanup old versions, keeping only the last N versions
     */
    private void cleanupOldVersions(Long knowledgeItemId) {
        final int MAX_VERSIONS = 10;

        List<KnowledgeVersion> versions = versionRepository
                .findByKnowledgeItemIdOrderByCreatedAtDesc(knowledgeItemId);

        if (versions.size() > MAX_VERSIONS) {
            List<KnowledgeVersion> toDelete = versions
                    .subList(MAX_VERSIONS, versions.size());
            versionRepository.deleteAll(toDelete);
        }
    }
}
