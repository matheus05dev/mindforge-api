package com.matheusdev.mindforge.study.resource.service;

import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.study.resource.dto.StudyResourceRequest;
import com.matheusdev.mindforge.study.resource.dto.StudyResourceResponse;
import com.matheusdev.mindforge.study.resource.mapper.StudyResourceMapper;
import com.matheusdev.mindforge.study.resource.model.StudyResource;
import com.matheusdev.mindforge.study.resource.repository.StudyResourceRepository;
import com.matheusdev.mindforge.study.subject.model.Subject;
import com.matheusdev.mindforge.study.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyResourceService {

    private final StudyResourceRepository resourceRepository;
    private final SubjectRepository subjectRepository;
    private final StudyResourceMapper mapper;

    public List<StudyResourceResponse> getResourcesBySubject(Long subjectId) {
        return resourceRepository.findBySubjectId(subjectId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudyResourceResponse createResource(Long subjectId, StudyResourceRequest request) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada com o id: " + subjectId));

        StudyResource resource = mapper.toEntity(request);
        resource.setSubject(subject);

        StudyResource savedResource = resourceRepository.save(resource);
        return mapper.toResponse(savedResource);
    }

    public void deleteResource(Long resourceId) {
        if (!resourceRepository.existsById(resourceId)) {
            throw new ResourceNotFoundException("Recurso não encontrado com o id: " + resourceId);
        }
        resourceRepository.deleteById(resourceId);
    }
}
