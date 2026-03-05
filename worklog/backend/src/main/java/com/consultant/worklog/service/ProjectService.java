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

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        log.debug("Fetching all projects");
        return projectRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(Long id) {
        log.debug("Fetching project with id: {}", id);
        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return convertToDTO(project);
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        log.debug("Creating new project: {}", projectDTO.getName());

        if (projectRepository.existsByName(projectDTO.getName())) {
            throw new DuplicateResourceException("Project already exists with name: " + projectDTO.getName());
        }

        Project project = Project.builder()
            .name(projectDTO.getName())
            .colorCode(projectDTO.getColorCode())
            .description(projectDTO.getDescription())
            .build();

        Project savedProject = projectRepository.save(project);
        log.info("Created project with id: {}", savedProject.getId());
        return convertToDTO(savedProject);
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO projectDTO) {
        log.debug("Updating project with id: {}", id);

        Project project = projectRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        if (!project.getName().equals(projectDTO.getName()) &&
            projectRepository.existsByName(projectDTO.getName())) {
            throw new DuplicateResourceException("Project already exists with name: " + projectDTO.getName());
        }

        project.setName(projectDTO.getName());
        project.setColorCode(projectDTO.getColorCode());
        project.setDescription(projectDTO.getDescription());

        Project updatedProject = projectRepository.save(project);
        log.info("Updated project with id: {}", updatedProject.getId());
        return convertToDTO(updatedProject);
    }

    @Transactional
    public void deleteProject(Long id, boolean deleteEntries) {
        log.debug("Deleting project with id: {}, deleteEntries: {}", id, deleteEntries);

        Project project = projectRepository.findById(id)
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
        log.info("Deleted project with id: {}", id);
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
