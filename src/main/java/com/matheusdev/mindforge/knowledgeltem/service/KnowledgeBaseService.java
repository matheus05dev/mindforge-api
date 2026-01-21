package com.matheusdev.mindforge.knowledgeltem.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeItem;
import com.matheusdev.mindforge.knowledgeltem.model.KnowledgeVersion;
import com.matheusdev.mindforge.knowledgeltem.repository.KnowledgeItemRepository;
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

    public List<KnowledgeItem> getAllItems() {
        return repository.findAll();
    }

    public KnowledgeItem getItemById(Long id) {
        return repository.findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Item de conhecimento não encontrado com o id: " + id));
    }

    public KnowledgeItem createItem(KnowledgeItem item, String workspaceId) {
        Workspace workspace = resolveWorkspace(workspaceId);
        item.setWorkspace(workspace);
        return repository.save(item);
    }

    private Workspace resolveWorkspace(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            // Try default "Geral"
            return workspaceRepository.findByNameIgnoreCase("Geral")
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Workspace ID not provided and default 'Geral' not found."));
        }

        // Try lookup by name (case insensitive)
        return workspaceRepository.findByNameIgnoreCase(identifier)
                .orElseGet(() -> {
                    // If not found by name, try as ID if numeric
                    try {
                        Long id = Long.parseLong(identifier);
                        return workspaceRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with ID: " + id));
                    } catch (NumberFormatException e) {
                        throw new ResourceNotFoundException("Workspace not found with identifier: " + identifier);
                    }
                });
    }

    public KnowledgeItem updateItem(Long id, KnowledgeItem item) {
        KnowledgeItem existingItem = getItemById(id);
        existingItem.setTitle(item.getTitle());
        existingItem.setContent(item.getContent());
        existingItem.setTags(item.getTags());
        return repository.save(existingItem);
    }

    public void deleteItem(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Item de conhecimento não encontrado com o id: " + id);
        }
        repository.deleteById(id);
    }

    public List<KnowledgeItem> searchByTag(String tag) {
        return repository.findByTag(tag);
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
                    // Create a copy or modify (since it comes from proposal, careful with cache
                    // side effects, but cache is usually fresh/serialized)
                    // Better to clone just to be safe if persistent
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

        // For simplicity, we'll apply changes sequentially
        // In a production system, you might want to handle conflicts and overlaps

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
                // Add content at specified position
                if (change.getStartLine() == 0) {
                    // Add at beginning
                    return change.getProposedContent() + "\n" + content;
                } else {
                    // Add at specific line
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
                // Remove content between startLine and endLine
                String[] lines = content.split("\n", -1);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.length; i++) {
                    if (i < change.getStartLine() || i > change.getEndLine()) {
                        sb.append(lines[i]);
                        if (i < lines.length - 1)
                            sb.append("\n");
                    }
                }
                return sb.toString();

            case REPLACE:
                // Replace content between startLine and endLine
                if (change.getOriginalContent() != null && !change.getOriginalContent().isEmpty()) {
                    // Replace by exact match
                    return content.replace(change.getOriginalContent(), change.getProposedContent());
                } else {
                    // Replace by line range
                    String[] contentLines = content.split("\n", -1);
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < contentLines.length; i++) {
                        if (i == change.getStartLine()) {
                            result.append(change.getProposedContent());
                            if (i < contentLines.length - 1)
                                result.append("\n");
                            // Skip lines until endLine
                            i = change.getEndLine();
                        } else {
                            result.append(contentLines[i]);
                            if (i < contentLines.length - 1)
                                result.append("\n");
                        }
                    }
                    return result.toString();
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
