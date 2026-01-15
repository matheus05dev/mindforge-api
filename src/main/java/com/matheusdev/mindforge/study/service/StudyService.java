package com.matheusdev.mindforge.study.service;


import com.matheusdev.mindforge.study.dto.StudySessionRequest;
import com.matheusdev.mindforge.study.dto.StudySessionResponse;
import com.matheusdev.mindforge.study.subject.dto.SubjectRequest;
import com.matheusdev.mindforge.study.subject.dto.SubjectResponse;
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
    private final StudyMapper mapper;

    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public SubjectResponse getSubjectById(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        return mapper.toResponse(subject);
    }

    public SubjectResponse createSubject(SubjectRequest request) {
        Subject subject = mapper.toEntity(request);
        Subject savedSubject = subjectRepository.save(subject);
        return mapper.toResponse(savedSubject);
    }

    @Transactional
    public SubjectResponse updateSubject(Long subjectId, SubjectRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        
        mapper.updateSubjectFromRequest(request, subject);
        Subject updatedSubject = subjectRepository.save(subject);
        return mapper.toResponse(updatedSubject);
    }

    public void deleteSubject(Long subjectId) {
        subjectRepository.deleteById(subjectId);
    }

    @Transactional
    public StudySessionResponse logSession(Long subjectId, StudySessionRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        StudySession session = mapper.toEntity(request);
        session.setSubject(subject);

        StudySession savedSession = studySessionRepository.save(session);
        return mapper.toResponse(savedSession);
    }

    @Transactional
    public StudySessionResponse updateSession(Long sessionId, StudySessionRequest request) {
        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        mapper.updateSessionFromRequest(request, session);
        StudySession updatedSession = studySessionRepository.save(session);
        return mapper.toResponse(updatedSession);
    }

    public void deleteSession(Long sessionId) {
        studySessionRepository.deleteById(sessionId);
    }
}
