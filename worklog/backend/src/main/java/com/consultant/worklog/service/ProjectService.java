package com.consultant.worklog.service;

import com.consultant.worklog.dto.ProjectDTO;
import com.consultant.worklog.model.Project;
import com.consultant.worklog.repository.ProjectRepository;
import com.consultant.worklog.repository.WorklogEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorklogEntryRepository worklogEntryRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Fetching all projects for user: {}", userId);
        return projectRepository.findByUserId(userId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Fetching project with id: {} for user: {}", id, userId);
        Project project = projectRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return convertToDTO(project);
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        var currentUser = authService.getCurrentUserEntity();
        log.debug("Creating new project: {} for user: {}", projectDTO.getName(), currentUser.getId());

        if (projectRepository.existsByNameAndUserId(projectDTO.getName(), currentUser.getId())) {
            throw new DuplicateResourceException("Project already exists with name: " + projectDTO.getName());
        }

        Project project = Project.builder()
            .name(projectDTO.getName())
            .colorCode(projectDTO.getColorCode())
            .description(projectDTO.getDescription())
            .user(currentUser)
            .build();

        Project savedProject = projectRepository.save(project);
        log.info("Created project with id: {} for user: {}", savedProject.getId(), currentUser.getId());
        return convertToDTO(savedProject);
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Updating project with id: {} for user: {}", id, userId);

        Project project = projectRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        if (!project.getName().equals(projectDTO.getName()) &&
            projectRepository.existsByNameAndUserId(projectDTO.getName(), userId)) {
            throw new DuplicateResourceException("Project already exists with name: " + projectDTO.getName());
        }

        project.setName(projectDTO.getName());
        project.setColorCode(projectDTO.getColorCode());
        project.setDescription(projectDTO.getDescription());

        Project updatedProject = projectRepository.save(project);
        log.info("Updated project with id: {} for user: {}", updatedProject.getId(), userId);
        return convertToDTO(updatedProject);
    }

    @Transactional
    public void deleteProject(Long id, boolean deleteEntries) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Deleting project with id: {} for user: {}, deleteEntries: {}", id, userId, deleteEntries);

        Project project = projectRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        // Check if project has entries
        long entryCount = worklogEntryRepository.countByProjectId(id);

        if (entryCount > 0 && !deleteEntries) {
            throw new ProjectHasEntriesException(
                "Project has " + entryCount + " worklog entries. Cannot delete without cascading."
            );
        }

        // If deleteEntries is true, delete all entries first
        if (deleteEntries && entryCount > 0) {
            worklogEntryRepository.deleteByProjectId(id);
            log.info("Deleted {} worklog entries for project {}", entryCount, id);
        }

        projectRepository.delete(project);
        log.info("Deleted project with id: {} for user: {}", id, userId);
    }

    @Transactional(readOnly = true)
    public long countEntriesByProject(Long projectId) {
        return worklogEntryRepository.countByProjectId(projectId);
    }

    private ProjectDTO convertToDTO(Project project) {
        return ProjectDTO.builder()
            .id(project.getId())
            .name(project.getName())
            .colorCode(project.getColorCode())
            .description(project.getDescription())
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }

    // Exception classes
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    public static class ProjectHasEntriesException extends RuntimeException {
        public ProjectHasEntriesException(String message) {
            super(message);
        }
    }
}
