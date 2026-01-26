package com.matheusdev.mindforge.study.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.study.dto.StudySessionRequest;
import com.matheusdev.mindforge.study.dto.StudySessionResponse;
import com.matheusdev.mindforge.study.subject.dto.SubjectRequest;
import com.matheusdev.mindforge.study.subject.dto.SubjectResponse;
import com.matheusdev.mindforge.study.subject.dto.SubjectSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.matheusdev.mindforge.study.mapper.StudyMapper;
import com.matheusdev.mindforge.study.model.StudySession;
import com.matheusdev.mindforge.study.repository.StudySessionRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final SubjectRepository subjectRepository;
    private final StudySessionRepository studySessionRepository;
    private final com.matheusdev.mindforge.workspace.repository.WorkspaceRepository workspaceRepository;
    private final StudyMapper mapper;

    @Transactional(readOnly = true)
    public Page<SubjectResponse> getAllSubjects(Pageable pageable, Long workspaceId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }

        if (workspaceId != null) {
            return subjectRepository.findByWorkspaceIdAndTenantId(workspaceId, tenantId, pageable)
                    .map(mapper::toResponse);
        }

        return subjectRepository.findByTenantId(tenantId, pageable)
                .map(mapper::toResponse);
    }

    public Page<SubjectSummaryResponse> getSubjectsByWorkspaceId(Long workspaceId, Pageable pageable) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        if (workspaceId == null) {
            throw new IllegalArgumentException("Workspace ID required for pagination");
        }

        return subjectRepository.findByWorkspaceIdAndTenantId(workspaceId, tenantId, pageable)
                .map(mapper::toSummaryResponse);
    }

    public SubjectResponse getSubjectById(Long subjectId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        Subject subject = subjectRepository.findByIdAndTenantId(subjectId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Assunto de estudo não encontrado com o id: " + subjectId));
        return mapper.toResponse(subject);
    }

    public SubjectResponse createSubject(SubjectRequest request) {
        Subject subject = mapper.toEntity(request);

        if (request.getWorkspaceId() != null) {
            com.matheusdev.mindforge.workspace.model.Workspace workspace = workspaceRepository
                    .findById(request.getWorkspaceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Workspace não encontrado com o id: " + request.getWorkspaceId()));
            subject.setWorkspace(workspace);
        } else {
            // Caso não venha workspaceId (ex: migração), poderia lançar erro ou pegar
            // default.
            // Como é obrigatório no banco, vou lançar erro se for null.
            throw new IllegalArgumentException("Workspace ID é obrigatório para criar uma matéria.");
        }

        Subject savedSubject = subjectRepository.save(subject);
        return mapper.toResponse(savedSubject);
    }

    @Transactional
    public SubjectResponse updateSubject(Long subjectId, SubjectRequest request) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        Subject subject = subjectRepository.findByIdAndTenantId(subjectId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Assunto de estudo não encontrado com o id: " + subjectId));

        mapper.updateSubjectFromRequest(request, subject);
        Subject updatedSubject = subjectRepository.save(subject);
        return mapper.toResponse(updatedSubject);
    }

    public void deleteSubject(Long subjectId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        Subject subject = subjectRepository.findByIdAndTenantId(subjectId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Assunto de estudo não encontrado com o id: " + subjectId));
        subjectRepository.delete(subject); // Use delete(entity) instead of deleteById
    }

    @Transactional
    public StudySessionResponse logSession(Long subjectId, StudySessionRequest request) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        Subject subject = subjectRepository.findByIdAndTenantId(subjectId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Assunto de estudo não encontrado com o id: " + subjectId));

        StudySession session = mapper.toEntity(request);
        session.setSubject(subject);

        StudySession savedSession = studySessionRepository.save(session);
        return mapper.toResponse(savedSession);
    }

    @Transactional
    public StudySessionResponse updateSession(Long sessionId, StudySessionRequest request) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        StudySession session = studySessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Sessão de estudo não encontrada com o id: " + sessionId));

        mapper.updateSessionFromRequest(request, session);
        StudySession updatedSession = studySessionRepository.save(session);
        return mapper.toResponse(updatedSession);
    }

    public void deleteSession(Long sessionId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        StudySession session = studySessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Sessão de estudo não encontrada com o id: " + sessionId));

        studySessionRepository.delete(session);
    }

    public List<StudySessionResponse> getSessionsBySubject(Long subjectId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        // verifica se o assunto pertence ao tenant
        if (!subjectRepository.findByIdAndTenantId(subjectId, tenantId).isPresent()) {
            throw new ResourceNotFoundException("Assunto de estudo não encontrado com o id: " + subjectId);
        }

        // assumindo que as sessões são filtradas pelo assunto que já foi validado,
        // mas robustamente deveríamos filtrar as sessões pelo tenant também se o
        // repositório suportasse.
        // studySessionRepository.findBySubjectIdAndTenantId...
        // No entanto, o repositório só tem findBySubjectId. Como o assunto foi validado
        // acima,
        // efetivamente estamos seguros SE as sessões não podem se mover entre
        // assuntos/tenants.
        // Melhor:
        // return studySessionRepository.findBySubjectId(subjectId).stream()...
        // O passo anterior implementou `findByTenantId` no repositório mas não
        // `findBySubjectIdAndTenantId`.
        // Vamos confiar na verificação de propriedade do assunto por enquanto.

        return studySessionRepository.findBySubjectId(subjectId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public StudySessionResponse getSessionById(Long sessionId) {
        Long tenantId = com.matheusdev.mindforge.core.tenant.context.TenantContext.getTenantId();
        StudySession session = studySessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Sessão de estudo não encontrada com o id: " + sessionId));
        return mapper.toResponse(session);
    }
}
