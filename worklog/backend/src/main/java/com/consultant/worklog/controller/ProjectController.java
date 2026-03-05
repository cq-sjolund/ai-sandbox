package com.consultant.worklog.controller;

import com.consultant.worklog.dto.ProjectDTO;
import com.consultant.worklog.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAllProjects() {
        log.debug("GET /api/projects - Fetching all projects");
        List<ProjectDTO> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Long id) {
        log.debug("GET /api/projects/{} - Fetching project by id", id);
        ProjectDTO project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO projectDTO) {
        log.debug("POST /api/projects - Creating new project");
        ProjectDTO createdProject = projectService.createProject(projectDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
        @PathVariable Long id,
        @Valid @RequestBody ProjectDTO projectDTO
    ) {
        log.debug("PUT /api/projects/{} - Updating project", id);
        ProjectDTO updatedProject = projectService.updateProject(id, projectDTO);
        return ResponseEntity.ok(updatedProject);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
        @PathVariable Long id,
        @RequestParam(required = false, defaultValue = "false") boolean deleteEntries
    ) {
        log.debug("DELETE /api/projects/{} - deleteEntries: {}", id, deleteEntries);
        projectService.deleteProject(id, deleteEntries);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/entries/count")
    public ResponseEntity<Long> countEntriesByProject(@PathVariable Long id) {
        log.debug("GET /api/projects/{}/entries/count", id);
        long count = projectService.countEntriesByProject(id);
        return ResponseEntity.ok(count);
    }
}
