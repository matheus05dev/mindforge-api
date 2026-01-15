package com.matheusdev.mindforge.project.service;

import com.matheusdev.mindforge.exception.BusinessException;
import com.matheusdev.mindforge.exception.ResourceNotFoundException;
import com.matheusdev.mindforge.project.dto.ProjectRequest;
import com.matheusdev.mindforge.project.dto.ProjectResponse;
import com.matheusdev.mindforge.project.mapper.ProjectMapper;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneRequest;
import com.matheusdev.mindforge.project.milestone.dto.MilestoneResponse;
import com.matheusdev.mindforge.project.milestone.model.Milestone;
import com.matheusdev.mindforge.project.milestone.repository.MilestoneRepository;
import com.matheusdev.mindforge.project.model.Project;
import com.matheusdev.mindforge.project.repository.ProjectRepository;
import com.matheusdev.mindforge.workspace.model.Workspace;
import com.matheusdev.mindforge.workspace.model.WorkspaceType;
import com.matheusdev.mindforge.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final WorkspaceService workspaceService;
    private final ProjectMapper mapper;

    public List<ProjectResponse> getAllProjects() {
        // Este método agora é ambíguo. Para um sistema multi-workspace,
        // deveríamos sempre listar projetos DENTRO de um workspace.
        // Por enquanto, vamos retornar todos, mas isso deve ser revisto.
        return projectRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ProjectResponse> getProjectsByWorkspaceId(Long workspaceId) {
        // Garante que o workspace existe antes de buscar os projetos
        workspaceService.findById(workspaceId);

        List<Project> projects = projectRepository.findAllByWorkspaceIdWithDetails(workspaceId);
        return projects.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o id: " + projectId));
        return mapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        Workspace workspace = workspaceService.findById(request.getWorkspaceId());

        if (workspace.getType() != WorkspaceType.PROJECT && workspace.getType() != WorkspaceType.GENERIC) {
            throw new BusinessException("Projetos só podem ser criados em workspaces do tipo PROJECT ou GENERIC.");
        }

        Project project = mapper.toEntity(request);
        project.setWorkspace(workspace);
        Project savedProject = projectRepository.save(project);
        return mapper.toResponse(savedProject);
    }

    @Transactional
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o id: " + projectId));
        
        if (request.getWorkspaceId() != null && !request.getWorkspaceId().equals(project.getWorkspace().getId())) {
             Workspace newWorkspace = workspaceService.findById(request.getWorkspaceId());
             if (newWorkspace.getType() != WorkspaceType.PROJECT && newWorkspace.getType() != WorkspaceType.GENERIC) {
                throw new BusinessException("Projetos não podem ser movidos para workspaces deste tipo.");
             }
             project.setWorkspace(newWorkspace);
        }
        
        mapper.updateProjectFromRequest(request, project);
        Project updatedProject = projectRepository.save(project);
        return mapper.toResponse(updatedProject);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Projeto não encontrado com o id: " + projectId);
        }
        projectRepository.deleteById(projectId);
    }

    @Transactional
    public ProjectResponse linkRepository(Long projectId, String repoUrl) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o id: " + projectId));
        project.setGithubRepoUrl(repoUrl);
        Project updatedProject = projectRepository.save(project);
        return mapper.toResponse(updatedProject);
    }

    @Transactional
    public MilestoneResponse addMilestone(Long projectId, MilestoneRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto não encontrado com o id: " + projectId));
        
        Milestone milestone = mapper.toEntity(request);
        milestone.setProject(project);
        Milestone savedMilestone = milestoneRepository.save(milestone);
        return mapper.toResponse(savedMilestone);
    }

    @Transactional
    public MilestoneResponse updateMilestone(Long milestoneId, MilestoneRequest request) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Marco (milestone) não encontrado com o id: " + milestoneId));
        
        mapper.updateMilestoneFromRequest(request, milestone);
        Milestone updatedMilestone = milestoneRepository.save(milestone);
        return mapper.toResponse(updatedMilestone);
    }

    @Transactional
    public void deleteMilestone(Long milestoneId) {
        if (!milestoneRepository.existsById(milestoneId)) {
            throw new ResourceNotFoundException("Marco (milestone) não encontrado com o id: " + milestoneId);
        }
        milestoneRepository.deleteById(milestoneId);
    }
}
