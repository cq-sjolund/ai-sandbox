package com.consultant.worklog.service;

import com.consultant.worklog.dto.DynamicsConfigDTO;
import com.consultant.worklog.dto.DynamicsTimeEntryDTO;
import com.consultant.worklog.model.DynamicsConfig;
import com.consultant.worklog.model.Project;
import com.consultant.worklog.model.User;
import com.consultant.worklog.model.WorklogEntry;
import com.consultant.worklog.repository.DynamicsConfigRepository;
import com.consultant.worklog.repository.ProjectRepository;
import com.consultant.worklog.repository.UserRepository;
import com.consultant.worklog.repository.WorklogEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicsService {

    private final DynamicsConfigRepository dynamicsConfigRepository;
    private final UserRepository userRepository;
    private final WorklogEntryRepository worklogEntryRepository;
    private final ProjectRepository projectRepository;
    private final OpenAIService openAIService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public DynamicsConfigDTO saveConfig(DynamicsConfigDTO configDTO) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        DynamicsConfig config = dynamicsConfigRepository.findByUserId(user.getId())
            .orElse(DynamicsConfig.builder().user(user).build());

        config.setOrganizationUrl(configDTO.getOrganizationUrl().trim());
        config.setAccessToken(configDTO.getAccessToken().trim());
        config.setBookableResourceId(configDTO.getBookableResourceId());
        config.setEnabled(configDTO.isEnabled());

        config = dynamicsConfigRepository.save(config);

        return mapToDTO(config);
    }

    @Transactional(readOnly = true)
    public Optional<DynamicsConfigDTO> getConfig() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return dynamicsConfigRepository.findByUserId(user.getId())
            .map(this::mapToDTO);
    }

    @Transactional
    public void deleteConfig() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        dynamicsConfigRepository.findByUserId(user.getId())
            .ifPresent(dynamicsConfigRepository::delete);
    }

    @Transactional
    public Map<String, Object> syncEntryToDynamics(Long entryId) {
        WorklogEntry entry = worklogEntryRepository.findById(entryId)
            .orElseThrow(() -> new IllegalArgumentException("Entry not found"));

        DynamicsConfig config = getDynamicsConfigForCurrentUser();

        try {
            log.info("Syncing entry {} to Dynamics", entryId);

            if (entry.getDynamicsId() != null) {
                // Update existing entry
                updateDynamicsTimeEntry(config, entry);
            } else {
                // Create new entry
                String dynamicsId = createDynamicsTimeEntry(config, entry);
                entry.setDynamicsId(dynamicsId);
            }

            entry.setLastSyncedAt(LocalDateTime.now());
            entry.setSyncStatus("SYNCED");
            worklogEntryRepository.save(entry);

            log.info("Successfully synced entry {} to Dynamics with ID: {}", entryId, entry.getDynamicsId());

            return Map.of(
                "success", true,
                "message", "Entry synced successfully",
                "dynamicsId", entry.getDynamicsId()
            );

        } catch (Exception e) {
            log.error("Failed to sync entry {} to Dynamics: {}", entryId, e.getMessage(), e);
            entry.setSyncStatus("FAILED");
            worklogEntryRepository.save(entry);

            return Map.of(
                "success", false,
                "message", "Failed to sync: " + e.getMessage()
            );
        }
    }

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

    @Transactional
    public Map<String, Object> importEntriesFromDynamics(LocalDate startDate, LocalDate endDate) {
        DynamicsConfig config = getDynamicsConfigForCurrentUser();

        try {
            log.info("Importing entries from Dynamics for date range: {} to {}", startDate, endDate);

            List<DynamicsTimeEntryDTO> dynamicsEntries = fetchDynamicsTimeEntries(config, startDate, endDate);

            int imported = 0;
            int skipped = 0;

            for (DynamicsTimeEntryDTO dynamicsEntry : dynamicsEntries) {
                LocalDate entryDate = LocalDate.parse(dynamicsEntry.getDate());

                // Check if entry already exists (by dynamics_id or by date)
                boolean exists = worklogEntryRepository.findAll().stream()
                    .anyMatch(e ->
                        (e.getDynamicsId() != null && e.getDynamicsId().equals(dynamicsEntry.getTimeEntryId())) ||
                        (e.getEntryDate().equals(entryDate) && e.getProject().getUser().getId().equals(config.getUser().getId()))
                    );

                if (!exists) {
                    // Import entry
                    importDynamicsEntry(config, dynamicsEntry);
                    imported++;
                } else {
                    skipped++;
                }
            }

            log.info("Imported {} entries from Dynamics, skipped {} existing entries", imported, skipped);

            return Map.of(
                "success", true,
                "imported", imported,
                "skipped", skipped,
                "message", String.format("Imported %d entries, skipped %d existing", imported, skipped)
            );

        } catch (Exception e) {
            log.error("Failed to import entries from Dynamics: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "message", "Failed to import: " + e.getMessage()
            );
        }
    }

    private String createDynamicsTimeEntry(DynamicsConfig config, WorklogEntry entry) throws Exception {
        String url = config.getOrganizationUrl() + "/api/data/v9.2/msdyn_timeentries";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("msdyn_date", entry.getEntryDate().toString());
        requestBody.put("msdyn_duration", entry.getHours().multiply(BigDecimal.valueOf(60)).intValue());
        requestBody.put("msdyn_description", entry.getSummary() + "\n\n" + entry.getDescription());
        requestBody.put("msdyn_type", 192350000); // Work
        requestBody.put("msdyn_entrystatus", 192350000); // Draft

        if (config.getBookableResourceId() != null && !config.getBookableResourceId().isBlank()) {
            requestBody.put("msdyn_bookableresource@odata.bind", "/bookableresources(" + config.getBookableResourceId() + ")");
        }

        HttpHeaders headers = createHeaders(config);
        headers.add("Prefer", "return=representation");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        log.info("Creating Dynamics time entry: POST {}", url);
        log.debug("Request body: {}", objectMapper.writeValueAsString(requestBody));

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        JsonNode responseNode = objectMapper.readTree(response.getBody());
        return responseNode.get("msdyn_timeentryid").asText();
    }

    private void updateDynamicsTimeEntry(DynamicsConfig config, WorklogEntry entry) throws Exception {
        String url = config.getOrganizationUrl() + "/api/data/v9.2/msdyn_timeentries(" + entry.getDynamicsId() + ")";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("msdyn_date", entry.getEntryDate().toString());
        requestBody.put("msdyn_duration", entry.getHours().multiply(BigDecimal.valueOf(60)).intValue());
        requestBody.put("msdyn_description", entry.getSummary() + "\n\n" + entry.getDescription());

        HttpHeaders headers = createHeaders(config);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        log.info("Updating Dynamics time entry: PATCH {}", url);
        log.debug("Request body: {}", objectMapper.writeValueAsString(requestBody));

        restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
    }

    private List<DynamicsTimeEntryDTO> fetchDynamicsTimeEntries(DynamicsConfig config, LocalDate startDate, LocalDate endDate) throws Exception {
        String filter = String.format("msdyn_date ge %s and msdyn_date le %s", startDate, endDate);

        // Use bookableResourceId from user if available
        String bookableResourceId = config.getUser().getBookableResourceId();
        if (bookableResourceId == null || bookableResourceId.isBlank()) {
            bookableResourceId = config.getBookableResourceId();
        }

        if (bookableResourceId != null && !bookableResourceId.isBlank()) {
            filter += " and _msdyn_bookableresource_value eq " + bookableResourceId;
        }

        String url = config.getOrganizationUrl() + "/api/data/v9.0/msdyn_timeentries?$filter=" + filter + "&$select=msdyn_timeentryid,msdyn_date,msdyn_duration,msdyn_description";

        HttpHeaders headers = createHeaders(config);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        log.info("Fetching Dynamics time entries: GET {}", url);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

        JsonNode responseNode = objectMapper.readTree(response.getBody());
        JsonNode values = responseNode.get("value");

        List<DynamicsTimeEntryDTO> entries = new ArrayList<>();
        if (values != null && values.isArray()) {
            for (JsonNode node : values) {
                DynamicsTimeEntryDTO dto = objectMapper.treeToValue(node, DynamicsTimeEntryDTO.class);
                entries.add(dto);
            }
        }

        log.info("Fetched {} entries from Dynamics", entries.size());
        return entries;
    }

    private void importDynamicsEntry(DynamicsConfig config, DynamicsTimeEntryDTO dynamicsEntry) {
        User user = config.getUser();

        // Get or create default project for user
        java.util.List<Project> userProjects = projectRepository.findByUserId(user.getId());
        Project defaultProject;
        if (userProjects == null || userProjects.isEmpty()) {
            // Create default project if user has none
            defaultProject = Project.builder()
                .name("Imported from Dynamics")
                .description("Default project for Dynamics imports")
                .colorCode("#1473E6")
                .user(user)
                .build();
            defaultProject = projectRepository.save(defaultProject);
            log.info("Created default project for user: {}", user.getUsername());
        } else {
            defaultProject = userProjects.get(0);
        }

        String description = dynamicsEntry.getDescription() != null ? dynamicsEntry.getDescription() : "";
        String summary = description.length() > 0
            ? description.substring(0, Math.min(255, description.length()))
            : "Imported from Dynamics";

        WorklogEntry entry = WorklogEntry.builder()
            .entryDate(LocalDate.parse(dynamicsEntry.getDate()))
            .hours(BigDecimal.valueOf(dynamicsEntry.getDuration()).divide(BigDecimal.valueOf(60)))
            .summary(summary)
            .description(description)
            .dynamicsId(dynamicsEntry.getTimeEntryId())
            .lastSyncedAt(LocalDateTime.now())
            .syncStatus("SYNCED")
            .project(defaultProject)
            .build();

        worklogEntryRepository.save(entry);
        log.info("Imported entry from Dynamics: {}", dynamicsEntry.getTimeEntryId());
    }

    private HttpHeaders createHeaders(DynamicsConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + config.getAccessToken());
        headers.add("OData-MaxVersion", "4.0");
        headers.add("OData-Version", "4.0");
        headers.add("Accept", "application/json");
        return headers;
    }

    private DynamicsConfig getDynamicsConfigForCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return dynamicsConfigRepository.findByUserId(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Dynamics configuration not found. Please configure Dynamics integration first."));
    }

    private DynamicsConfigDTO mapToDTO(DynamicsConfig config) {
        return DynamicsConfigDTO.builder()
            .id(config.getId())
            .organizationUrl(config.getOrganizationUrl())
            .accessToken(maskToken(config.getAccessToken()))
            .bookableResourceId(config.getBookableResourceId())
            .enabled(config.isEnabled())
            .build();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
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
