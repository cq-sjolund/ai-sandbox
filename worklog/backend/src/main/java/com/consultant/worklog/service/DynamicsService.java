package com.consultant.worklog.service;

import com.consultant.worklog.model.Project;
import com.consultant.worklog.model.User;
import com.consultant.worklog.model.WorklogEntry;
import com.consultant.worklog.repository.ProjectRepository;
import com.consultant.worklog.repository.UserRepository;
import com.consultant.worklog.repository.WorklogEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicsService {

    private final UserRepository userRepository;
    private final WorklogEntryRepository worklogEntryRepository;
    private final ProjectRepository projectRepository;
    private final OpenAIService openAIService;

    @Transactional(readOnly = true)
    public Map<String, Object> analyzeProjectMappings(java.util.List<java.util.Map<String, Object>> entriesData) {
        try {
            log.info("Analyzing project mappings for {} entries", entriesData.size());

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Get existing user projects
            java.util.List<Project> userProjects = projectRepository.findByUserId(user.getId());
            java.util.List<String> existingProjectNames = userProjects.stream()
                .map(Project::getName)
                .toList();

            log.info("User has {} existing projects: {}", existingProjectNames.size(), existingProjectNames);

            // Extract unique Dynamics project names from entries
            Set<String> uniqueDynamicsProjects = new HashSet<>();
            for (java.util.Map<String, Object> entryData : entriesData) {
                String projectName = (String) entryData.get("project");
                if (projectName != null && !projectName.isBlank()) {
                    uniqueDynamicsProjects.add(projectName);
                }
            }

            log.info("Found {} unique Dynamics projects to map", uniqueDynamicsProjects.size());

            // Analyze each unique project with AI
            java.util.List<Map<String, Object>> mappings = new ArrayList<>();
            for (String dynamicsProjectName : uniqueDynamicsProjects) {
                OpenAIService.ProjectMappingResult mappingResult =
                    openAIService.mapDynamicsProjectName(dynamicsProjectName, existingProjectNames);

                Map<String, Object> mapping = new HashMap<>();
                mapping.put("dynamicsProjectName", dynamicsProjectName);
                mapping.put("suggestedProjectName", mappingResult.getMatchedProjectName());
                mapping.put("confidence", mappingResult.getConfidence());
                mapping.put("reason", mappingResult.getReason());
                mapping.put("requiresConfirmation", !mappingResult.isHighConfidence());

                mappings.add(mapping);

                log.info("Mapping: {} -> {} (confidence: {})",
                    dynamicsProjectName,
                    mappingResult.getMatchedProjectName(),
                    mappingResult.getConfidence());
            }

            return Map.of(
                "success", true,
                "mappings", mappings,
                "existingProjects", existingProjectNames
            );

        } catch (Exception e) {
            log.error("Failed to analyze project mappings: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Failed to analyze: " + e.getMessage()
            );
        }
    }

    @Transactional
    public Map<String, Object> importEntriesFromFrontend(java.util.List<java.util.Map<String, Object>> entriesData,
                                                          Map<String, String> projectMappings) {
        try {
            log.info("Importing {} entries from frontend with {} project mappings",
                entriesData.size(), projectMappings.size());

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            int imported = 0;
            int skipped = 0;

            // Build a map of project names to project objects for this user
            java.util.Map<String, Project> projectMap = new java.util.HashMap<>();
            java.util.List<Project> userProjects = projectRepository.findByUserId(user.getId());
            for (Project p : userProjects) {
                projectMap.put(p.getName().toLowerCase(), p);
            }

            for (java.util.Map<String, Object> entryData : entriesData) {
                try {
                    String dynamicsId = (String) entryData.get("dynamicsId");
                    String dateStr = (String) entryData.get("date");
                    Object hoursObj = entryData.get("hours");
                    String description = (String) entryData.get("description");
                    String dynamicsProjectName = (String) entryData.get("project");

                    if (dynamicsId == null || dateStr == null || hoursObj == null) {
                        log.warn("Skipping entry with missing data: {}", entryData);
                        skipped++;
                        continue;
                    }

                    LocalDate entryDate = LocalDate.parse(dateStr);
                    BigDecimal hours = BigDecimal.valueOf(((Number) hoursObj).doubleValue());

                    // Check if entry already exists
                    boolean exists = worklogEntryRepository.findAll().stream()
                        .anyMatch(e ->
                            (e.getDynamicsId() != null && e.getDynamicsId().equals(dynamicsId)) ||
                            (e.getEntryDate().equals(entryDate) &&
                             e.getProject().getUser().getId().equals(user.getId()) &&
                             Math.abs(e.getHours().subtract(hours).doubleValue()) < 0.1)
                        );

                    if (exists) {
                        log.debug("Entry already exists: {}", dynamicsId);
                        skipped++;
                        continue;
                    }

                    // Check if this day already has entries for this user
                    boolean dayHasEntries = worklogEntryRepository.findAll().stream()
                        .anyMatch(e -> e.getProject().getUser().getId().equals(user.getId()) &&
                                      e.getEntryDate().equals(entryDate));

                    if (dayHasEntries) {
                        log.info("Skipping entry for {} - day already has entries", entryDate);
                        skipped++;
                        continue;
                    }

                    // Determine project using provided mappings
                    String targetProjectName;
                    String mappedProjectName = null;
                    if (dynamicsProjectName != null && !dynamicsProjectName.isBlank()) {
                        // Check if user provided a mapping for this Dynamics project
                        mappedProjectName = projectMappings.get(dynamicsProjectName);
                        if (mappedProjectName != null && !mappedProjectName.equals(dynamicsProjectName)) {
                            // AI mapped to existing project - use the more descriptive name
                            // If Dynamics name is longer/more descriptive, rename the existing project
                            targetProjectName = dynamicsProjectName.length() > mappedProjectName.length()
                                ? dynamicsProjectName
                                : mappedProjectName;
                        } else {
                            targetProjectName = dynamicsProjectName;
                        }
                    } else {
                        targetProjectName = "Imported from Dynamics";
                    }

                    // Get or create project
                    Project project = projectMap.get(targetProjectName.toLowerCase());
                    if (project == null) {
                        // Check if we're renaming an existing project
                        if (mappedProjectName != null && !mappedProjectName.equals(dynamicsProjectName)) {
                            Project existingProject = projectMap.get(mappedProjectName.toLowerCase());
                            if (existingProject != null && dynamicsProjectName.length() > mappedProjectName.length()) {
                                // Rename existing project to more descriptive name
                                log.info("Renaming project '{}' to more descriptive '{}'",
                                    existingProject.getName(), targetProjectName);
                                existingProject.setName(targetProjectName);
                                project = projectRepository.save(existingProject);
                                projectMap.remove(mappedProjectName.toLowerCase());
                                projectMap.put(targetProjectName.toLowerCase(), project);
                            } else if (existingProject != null) {
                                // Use existing project as-is
                                project = existingProject;
                            }
                        }

                        // Create new project if still null
                        if (project == null) {
                            String colorCode;
                            try {
                                colorCode = openAIService.suggestProjectColor(targetProjectName);
                            } catch (Exception e) {
                                log.warn("Failed to get AI color suggestion, using default: {}", e.getMessage());
                                colorCode = generateColorForProject(targetProjectName);
                            }

                            project = Project.builder()
                                .name(targetProjectName)
                                .description("Imported from Dynamics")
                                .colorCode(colorCode)
                                .user(user)
                                .build();
                            project = projectRepository.save(project);
                            projectMap.put(targetProjectName.toLowerCase(), project);
                            log.info("Created new project '{}' for user: {}", targetProjectName, user.getUsername());
                        }
                    }

                    Project defaultProject = project;

                    // Create worklog entry
                    String summary = description != null && description.length() > 0
                        ? description.substring(0, Math.min(255, description.length()))
                        : "Imported from Dynamics";

                    WorklogEntry entry = WorklogEntry.builder()
                        .entryDate(entryDate)
                        .hours(hours)
                        .summary(summary)
                        .description(description != null ? description : "")
                        .project(defaultProject)
                        .dynamicsId(dynamicsId)
                        .lastSyncedAt(LocalDateTime.now())
                        .syncStatus("SYNCED")
                        .build();

                    worklogEntryRepository.save(entry);
                    imported++;
                    log.debug("Imported entry: {}", dynamicsId);

                } catch (Exception e) {
                    log.error("Error importing individual entry: {}", e.getMessage(), e);
                    skipped++;
                }
            }

            log.info("Import complete: {} imported, {} skipped", imported, skipped);

            return Map.of(
                "success", true,
                "imported", imported,
                "skipped", skipped,
                "message", String.format("Imported %d entries, skipped %d existing", imported, skipped)
            );

        } catch (Exception e) {
            log.error("Failed to import entries: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Failed to import: " + e.getMessage(),
                "imported", 0,
                "skipped", 0
            );
        }
    }

    private String generateColorForProject(String projectName) {
        // Generate a consistent color based on project name hash
        String[] colors = {
            "#1473E6", // Adobe Blue
            "#E34850", // Red
            "#2D9D78", // Green
            "#9256D9", // Purple
            "#E68619", // Orange
            "#D83790", // Pink
            "#0D66D0", // Dark Blue
            "#268E6C"  // Teal
        };
        int hash = Math.abs(projectName.hashCode());
        return colors[hash % colors.length];
    }
}
