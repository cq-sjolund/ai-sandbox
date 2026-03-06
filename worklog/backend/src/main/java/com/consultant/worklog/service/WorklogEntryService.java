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
    private final AuthService authService;

    @Transactional(readOnly = true)
    public List<WorklogEntryDTO> getAllEntries() {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Fetching all worklog entries for user: {}", userId);
        return worklogEntryRepository.findByUserId(userId).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorklogEntryDTO getEntryById(Long id) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Fetching worklog entry with id: {} for user: {}", id, userId);
        WorklogEntry entry = worklogEntryRepository.findById(id)
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id));

        // Verify the entry belongs to the user through the project
        if (!entry.getProject().getUser().getId().equals(userId)) {
            throw new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id);
        }

        return convertToDTO(entry);
    }

    @Transactional(readOnly = true)
    public List<WorklogEntryDTO> getEntriesByDate(LocalDate date) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Fetching worklog entries for date: {} for user: {}", date, userId);
        return worklogEntryRepository.findByUserIdAndEntryDate(userId, date).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorklogEntryDTO> getEntriesByDateRange(LocalDate startDate, LocalDate endDate) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Fetching worklog entries between {} and {} for user: {}", startDate, endDate, userId);
        return worklogEntryRepository.findByUserIdAndEntryDateBetween(userId, startDate, endDate).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public WorklogEntryDTO createEntry(WorklogEntryDTO entryDTO) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Creating new worklog entry for date: {} for user: {}", entryDTO.getEntryDate(), userId);

        // Verify the project belongs to the current user
        Project project = projectRepository.findByIdAndUserId(entryDTO.getProjectId(), userId)
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Project not found with id: " + entryDTO.getProjectId()));

        WorklogEntry entry = WorklogEntry.builder()
            .entryDate(entryDTO.getEntryDate())
            .summary(entryDTO.getSummary())
            .description(entryDTO.getDescription())
            .hours(entryDTO.getHours())
            .project(project)
            .build();

        WorklogEntry savedEntry = worklogEntryRepository.save(entry);
        log.info("Created worklog entry with id: {} for user: {}", savedEntry.getId(), userId);
        return convertToDTO(savedEntry);
    }

    @Transactional
    public WorklogEntryDTO updateEntry(Long id, WorklogEntryDTO entryDTO) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Updating worklog entry with id: {} for user: {}", id, userId);

        WorklogEntry entry = worklogEntryRepository.findById(id)
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id));

        // Verify the entry belongs to the user through the project
        if (!entry.getProject().getUser().getId().equals(userId)) {
            throw new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id);
        }

        // Verify the new project also belongs to the user
        Project project = projectRepository.findByIdAndUserId(entryDTO.getProjectId(), userId)
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Project not found with id: " + entryDTO.getProjectId()));

        entry.setEntryDate(entryDTO.getEntryDate());
        entry.setSummary(entryDTO.getSummary());
        entry.setDescription(entryDTO.getDescription());
        entry.setHours(entryDTO.getHours());
        entry.setProject(project);

        WorklogEntry updatedEntry = worklogEntryRepository.save(entry);
        log.info("Updated worklog entry with id: {} for user: {}", updatedEntry.getId(), userId);
        return convertToDTO(updatedEntry);
    }

    @Transactional
    public void deleteEntry(Long id) {
        Long userId = authService.getCurrentUserEntity().getId();
        log.debug("Deleting worklog entry with id: {} for user: {}", id, userId);

        WorklogEntry entry = worklogEntryRepository.findById(id)
            .orElseThrow(() -> new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id));

        // Verify the entry belongs to the user through the project
        if (!entry.getProject().getUser().getId().equals(userId)) {
            throw new ProjectService.ResourceNotFoundException("Worklog entry not found with id: " + id);
        }

        worklogEntryRepository.deleteById(id);
        log.info("Deleted worklog entry with id: {} for user: {}", id, userId);
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
            .dynamicsId(entry.getDynamicsId())
            .lastSyncedAt(entry.getLastSyncedAt())
            .syncStatus(entry.getSyncStatus())
            .createdAt(entry.getCreatedAt())
            .updatedAt(entry.getUpdatedAt())
            .build();
    }
}
