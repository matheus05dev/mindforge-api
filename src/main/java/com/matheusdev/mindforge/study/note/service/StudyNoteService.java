package com.matheusdev.mindforge.study.note.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.study.note.dto.NoteRequest;
import com.matheusdev.mindforge.study.note.dto.NoteResponse;
import com.matheusdev.mindforge.study.note.dto.StudyNoteAIRequest;
import com.matheusdev.mindforge.study.note.dto.StudyNoteAIResponse;
import com.matheusdev.mindforge.study.note.mapper.StudyNoteMapper;
import com.matheusdev.mindforge.study.note.model.Note;
import com.matheusdev.mindforge.study.note.repository.StudyNoteRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import com.matheusdev.mindforge.study.note.repository.StudyNoteVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyNoteService {

    private final StudyNoteRepository noteRepository;
    private final SubjectRepository subjectRepository;
    private final StudyNoteMapper mapper;
    private final com.matheusdev.mindforge.ai.service.AIOrchestrationService aiOrchestrationService;
    private final StudyNoteVersionRepository versionRepository;

    public List<NoteResponse> getNotesBySubject(Long subjectId) {
        return noteRepository.findBySubjectIdOrderByUpdatedAtDesc(subjectId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public NoteResponse getNoteById(Long noteId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada com o id: " + noteId));
        return mapper.toResponse(note);
    }

    @Transactional
    public NoteResponse createNote(Long subjectId, NoteRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada com o id: " + subjectId));

        Note note = mapper.toEntity(request);
        note.setSubject(subject);

        Note savedNote = noteRepository.save(note);

        // Save initial version
        saveVersion(savedNote.getId(), null,
                com.matheusdev.mindforge.study.note.model.StudyNoteVersion.ChangeType.INITIAL_VERSION,
                "Initial version");

        return mapper.toResponse(savedNote);
    }

    @Transactional
    public NoteResponse updateNote(Long noteId, NoteRequest request) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada com o id: " + noteId));

        // Save version before update
        saveVersion(noteId, null, com.matheusdev.mindforge.study.note.model.StudyNoteVersion.ChangeType.MANUAL_EDIT,
                "Manual edit");

        mapper.updateNoteFromRequest(request, note);
        Note updatedNote = noteRepository.save(note);
        return mapper.toResponse(updatedNote);
    }

    public void deleteNote(Long noteId) {
        if (!noteRepository.existsById(noteId)) {
            throw new ResourceNotFoundException("Nota não encontrada com o id: " + noteId);
        }
        // Delete versions first? Or Cascade? Assuming database FK Cascade or manual
        // delete
        // Implementing manual delete for safety if cascade not configured
        versionRepository.deleteByStudyNoteId(noteId);
        noteRepository.deleteById(noteId);
    }

    /**
     * Processa requisições de IA para notas de estudo.
     * Delega para o AIOrchestrationService.
     */
    public CompletableFuture<StudyNoteAIResponse> processAIRequest(StudyNoteAIRequest request) {
        return aiOrchestrationService.processStudyNoteAssist(request);
    }

    /**
     * Aplica uma proposta aprovada do agente à nota de estudo.
     */
    @Transactional
    public NoteResponse applyProposal(
            com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeAgentProposal proposal,
            com.matheusdev.mindforge.knowledgeltem.dto.ApprovalRequest approval) {

        // 1. Get the note
        Note note = noteRepository.findById(proposal.getKnowledgeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nota não encontrada com o id: " + proposal.getKnowledgeId()));

        String currentContent = note.getContent() != null ? note.getContent() : "";

        // 2. Save version BEFORE applying changes
        String changeSummary = approval.isApproveAll()
                ? "Applied all changes from agent proposal"
                : String.format("Applied %d selected changes from agent proposal",
                        approval.getApprovedChangeIndices() != null ? approval.getApprovedChangeIndices().size() : 0);

        saveVersion(
                note.getId(),
                proposal.getProposalId(),
                com.matheusdev.mindforge.study.note.model.StudyNoteVersion.ChangeType.AGENT_PROPOSAL,
                changeSummary);

        // 3. Determine which changes to apply
        List<com.matheusdev.mindforge.knowledgeltem.dto.ContentChange> changesToApply = new java.util.ArrayList<>();

        if (approval.isApproveAll()) {
            // Add all, respecting manual edits
            for (int i = 0; i < proposal.getChanges().size(); i++) {
                com.matheusdev.mindforge.knowledgeltem.dto.ContentChange change = proposal.getChanges().get(i);

                if (approval.getModifiedContent() != null && approval.getModifiedContent().containsKey(i)) {
                    com.matheusdev.mindforge.knowledgeltem.dto.ContentChange modifiedChange = new com.matheusdev.mindforge.knowledgeltem.dto.ContentChange();
                    modifiedChange.setType(change.getType());
                    modifiedChange.setStartLine(change.getStartLine());
                    modifiedChange.setEndLine(change.getEndLine());
                    modifiedChange.setOriginalContent(change.getOriginalContent());
                    modifiedChange.setReason(change.getReason());
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

                        if (approval.getModifiedContent() != null && approval.getModifiedContent().containsKey(index)) {
                            com.matheusdev.mindforge.knowledgeltem.dto.ContentChange modifiedChange = new com.matheusdev.mindforge.knowledgeltem.dto.ContentChange();
                            modifiedChange.setType(change.getType());
                            modifiedChange.setStartLine(change.getStartLine());
                            modifiedChange.setEndLine(change.getEndLine());
                            modifiedChange.setOriginalContent(change.getOriginalContent());
                            modifiedChange.setReason(change.getReason());
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

        // 5. Update the note
        note.setContent(newContent);
        Note updatedNote = noteRepository.save(note);

        return mapper.toResponse(updatedNote);
    }

    // ==================== VERSION MANAGEMENT ====================

    public void saveVersion(Long noteId, String proposalId,
            com.matheusdev.mindforge.study.note.model.StudyNoteVersion.ChangeType changeType,
            String changeSummary) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada: " + noteId));

        com.matheusdev.mindforge.study.note.model.StudyNoteVersion version = com.matheusdev.mindforge.study.note.model.StudyNoteVersion
                .builder()
                .studyNoteId(noteId)
                .title(note.getTitle())
                .content(note.getContent())
                .changeType(changeType)
                .proposalId(proposalId)
                .changeSummary(changeSummary)
                .build();

        versionRepository.save(version);
        cleanupOldVersions(noteId);
    }

    public List<com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse> getVersionHistory(Long noteId) {
        // Reuse KnowledgeVersionResponse DTO as structure is identical
        // Ideally should create proper StudyNoteVersionResponse
        List<com.matheusdev.mindforge.study.note.model.StudyNoteVersion> versions = versionRepository
                .findByStudyNoteIdOrderByCreatedAtDesc(noteId);

        return versions.stream()
                .map(v -> {
                    com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse resp = new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse();
                    resp.setId(v.getId());
                    resp.setKnowledgeItemId(v.getStudyNoteId());
                    resp.setTitle(v.getTitle());
                    // resp.setContentPreview(v.getContent() != null ? v.getContent().substring(0,
                    // Math.min(v.getContent().length(), 100)) : "");
                    // Just basic mapping for now
                    resp.setCreatedAt(v.getCreatedAt());
                    resp.setChangeType(com.matheusdev.mindforge.knowledgeltem.model.KnowledgeVersion.ChangeType
                            .valueOf(v.getChangeType().name()));
                    resp.setChangeSummary(v.getChangeSummary());
                    return resp;
                })
                .collect(Collectors.toList());
    }

    public com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse getVersion(Long versionId) {
        com.matheusdev.mindforge.study.note.model.StudyNoteVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Versão não encontrada: " + versionId));

        com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse resp = new com.matheusdev.mindforge.knowledgeltem.dto.KnowledgeVersionResponse();
        resp.setId(version.getId());
        resp.setKnowledgeItemId(version.getStudyNoteId());
        resp.setTitle(version.getTitle());
        resp.setFullContent(version.getContent());
        resp.setCreatedAt(version.getCreatedAt());
        resp.setChangeType(com.matheusdev.mindforge.knowledgeltem.model.KnowledgeVersion.ChangeType
                .valueOf(version.getChangeType().name()));
        resp.setChangeSummary(version.getChangeSummary());
        return resp;
    }

    @Transactional
    public NoteResponse rollbackToVersion(Long noteId, Long versionId) {
        com.matheusdev.mindforge.study.note.model.StudyNoteVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Versão não encontrada: " + versionId));

        if (!version.getStudyNoteId().equals(noteId)) {
            throw new IllegalArgumentException("Versão não pertence a esta nota");
        }

        // Save current state before rollback
        saveVersion(noteId, null, com.matheusdev.mindforge.study.note.model.StudyNoteVersion.ChangeType.ROLLBACK,
                "Before rollback to version " + versionId);

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nota não encontrada: " + noteId));

        note.setTitle(version.getTitle());
        note.setContent(version.getContent());

        Note updated = noteRepository.save(note);
        return mapper.toResponse(updated);
    }

    private void cleanupOldVersions(Long noteId) {
        final int MAX_VERSIONS = 20;
        List<com.matheusdev.mindforge.study.note.model.StudyNoteVersion> versions = versionRepository
                .findByStudyNoteIdOrderByCreatedAtDesc(noteId);

        if (versions.size() > MAX_VERSIONS) {
            List<com.matheusdev.mindforge.study.note.model.StudyNoteVersion> toDelete = versions
                    .subList(MAX_VERSIONS, versions.size());
            versionRepository.deleteAll(toDelete);
        }
    }

    /**
     * Aplica a lista de mudanças ao conteúdo original.
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
     * Aplica uma única mudança.
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
                if (change.getOriginalContent() != null && !change.getOriginalContent().isEmpty()) {
                    // Simple replace if content matches
                    return content.replace(change.getOriginalContent(), change.getProposedContent());
                } else {
                    // Line range replace
                    String[] contentLines = content.split("\n", -1);
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < contentLines.length; i++) {
                        if (i == change.getStartLine()) {
                            result.append(change.getProposedContent());
                            if (i < contentLines.length - 1)
                                result.append("\n");
                            i = change.getEndLine(); // Skip lines
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
}
