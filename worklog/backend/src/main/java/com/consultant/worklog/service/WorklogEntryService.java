package com.consultant.worklog.service;

import com.consultant.worklog.dto.ProjectDTO;
import com.consultant.worklog.dto.WorklogEntryDTO;
import com.consultant.worklog.model.Project;
import com.consultant.worklog.model.WorklogEntry;
import com.consultant.worklog.repository.ProjectRepository;
import com.consultant.worklog.repository.WorklogEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorklogEntryService {

    private final WorklogEntryRepository worklogEntryRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<WorklogEntryDTO> getAllEntries() {
        log.debug("Fetching all worklog entries");
        return worklogEntryRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorklogEntryDTO getEntryById(Long id) {
        log.debug("Fetching worklog entry with id: {}", id);
        WorklogEntry entry = worklogEntryRepository.findById(id)
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id));
        return convertToDTO(entry);
    }

    @Transactional(readOnly = true)
    public List<WorklogEntryDTO> getEntriesByDate(LocalDate date) {
        log.debug("Fetching worklog entries for date: {}", date);
        return worklogEntryRepository.findByEntryDateOrderByCreatedAtDesc(date).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorklogEntryDTO> getEntriesByDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching worklog entries between {} and {}", startDate, endDate);
        return worklogEntryRepository.findByEntryDateBetweenOrderByEntryDateDesc(startDate, endDate).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public WorklogEntryDTO createEntry(WorklogEntryDTO entryDTO) {
        log.debug("Creating new worklog entry for date: {}", entryDTO.getEntryDate());

        Project project = projectRepository.findById(entryDTO.getProjectId())
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Project not found with id: " + entryDTO.getProjectId()));

        WorklogEntry entry = WorklogEntry.builder()
            .entryDate(entryDTO.getEntryDate())
            .summary(entryDTO.getSummary())
            .description(entryDTO.getDescription())
            .hours(entryDTO.getHours())
            .project(project)
            .build();

        WorklogEntry savedEntry = worklogEntryRepository.save(entry);
        log.info("Created worklog entry with id: {}", savedEntry.getId());
        return convertToDTO(savedEntry);
    }

    @Transactional
    public WorklogEntryDTO updateEntry(Long id, WorklogEntryDTO entryDTO) {
        log.debug("Updating worklog entry with id: {}", id);

        WorklogEntry entry = worklogEntryRepository.findById(id)
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id));

        Project project = projectRepository.findById(entryDTO.getProjectId())
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Project not found with id: " + entryDTO.getProjectId()));

        entry.setEntryDate(entryDTO.getEntryDate());
        entry.setSummary(entryDTO.getSummary());
        entry.setDescription(entryDTO.getDescription());
        entry.setHours(entryDTO.getHours());
        entry.setProject(project);

        WorklogEntry updatedEntry = worklogEntryRepository.save(entry);
        log.info("Updated worklog entry with id: {}", updatedEntry.getId());
        return convertToDTO(updatedEntry);
    }

    @Transactional
    public void deleteEntry(Long id) {
        log.debug("Deleting worklog entry with id: {}", id);

        if (!worklogEntryRepository.existsById(id)) {
            throw new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id);
        }

        worklogEntryRepository.deleteById(id);
        log.info("Deleted worklog entry with id: {}", id);
    }

    private WorklogEntryDTO convertToDTO(WorklogEntry entry) {
        ProjectDTO projectDTO = ProjectDTO.builder()
            .id(entry.getProject().getId())
            .name(entry.getProject().getName())
            .colorCode(entry.getProject().getColorCode())
            .description(entry.getProject().getDescription())
            .build();

        return WorklogEntryDTO.builder()
            .id(entry.getId())
            .entryDate(entry.getEntryDate())
            .summary(entry.getSummary())
            .description(entry.getDescription())
            .hours(entry.getHours())
            .projectId(entry.getProject().getId())
            .project(projectDTO)
            .createdAt(entry.getCreatedAt())
            .updatedAt(entry.getUpdatedAt())
            .build();
    }
}
