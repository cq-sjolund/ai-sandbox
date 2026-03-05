package com.consultant.worklog.service;

import com.consultant.worklog.dto.AISummaryRequestDTO;
import com.consultant.worklog.model.WorklogEntry;
import com.consultant.worklog.repository.WorklogEntryRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OpenAIService {

    private final WorklogEntryRepository worklogEntryRepository;
    private final OpenAiService openAiService;
    private final String model;

    public OpenAIService(
        WorklogEntryRepository worklogEntryRepository,
        @Value("${openai.api.key}") String apiKey,
        @Value("${openai.api.model}") String model
    ) {
        this.worklogEntryRepository = worklogEntryRepository;
        this.model = model;
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
        log.info("OpenAI Service initialized with model: {}", model);
    }

    public String generateSummary(AISummaryRequestDTO request) {
        log.debug("Generating AI summary for date range: {} to {}", request.getDateRangeStart(), request.getDateRangeEnd());

        List<WorklogEntry> entries = worklogEntryRepository.findEntriesForSummary(
            request.getDateRangeStart(),
            request.getDateRangeEnd(),
            request.getProjectIds()
        );

        if (entries.isEmpty()) {
            return "No worklog entries found for the specified date range.";
        }

        String prompt = buildPrompt(entries, request);
        return callOpenAI(prompt);
    }

    private String buildPrompt(List<WorklogEntry> entries, AISummaryRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional consultant assistant. Generate a concise summary of the following worklog entries.\n\n");

        if (request.getCustomPrompt() != null && !request.getCustomPrompt().isBlank()) {
            prompt.append("Additional instructions: ").append(request.getCustomPrompt()).append("\n\n");
        }

        prompt.append("Period: ").append(formatDate(request.getDateRangeStart()))
              .append(" to ").append(formatDate(request.getDateRangeEnd())).append("\n\n");

        // Group entries by project
        Map<String, List<WorklogEntry>> entriesByProject = entries.stream()
            .collect(Collectors.groupingBy(e -> e.getProject().getName()));

        // Calculate total hours
        BigDecimal totalHours = entries.stream()
            .map(WorklogEntry::getHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        prompt.append("Total Hours Logged: ").append(totalHours).append("\n\n");

        // Add entries grouped by project
        prompt.append("Worklog Entries by Project:\n\n");
        for (Map.Entry<String, List<WorklogEntry>> projectEntry : entriesByProject.entrySet()) {
            String projectName = projectEntry.getKey();
            List<WorklogEntry> projectEntries = projectEntry.getValue();

            BigDecimal projectHours = projectEntries.stream()
                .map(WorklogEntry::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            prompt.append("## ").append(projectName).append(" (").append(projectHours).append(" hours)\n\n");

            for (WorklogEntry entry : projectEntries) {
                prompt.append("- **").append(formatDate(entry.getEntryDate())).append("** (")
                      .append(entry.getHours()).append("h): ").append(entry.getSummary()).append("\n");
                if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
                    prompt.append("  ").append(entry.getDescription()).append("\n");
                }
                prompt.append("\n");
            }
        }

        prompt.append("\nPlease provide:\n");
        prompt.append("1. A high-level summary of work completed\n");
        prompt.append("2. Key achievements and deliverables\n");
        prompt.append("3. Hours breakdown by project\n");
        prompt.append("4. Any notable patterns or observations\n");

        return prompt.toString();
    }

    private String callOpenAI(String prompt) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                "You are a professional consultant assistant who helps summarize worklog entries."));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(1000)
                .temperature(0.7)
                .build();

            String response = openAiService.createChatCompletion(completionRequest)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent();

            log.info("Successfully generated AI summary");
            return response;

        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate AI summary: " + e.getMessage(), e);
        }
    }

    public String suggestProjectColor(String projectName) {
        log.debug("Suggesting color for project: {}", projectName);

        try {
            String prompt = "You are a color expert. Based on the project name \"" + projectName +
                "\", suggest an appropriate hex color code (e.g., #FF5733). " +
                "Consider common color associations (e.g., Marketing=orange/red, Development=blue, Sales=green, HR=purple). " +
                "Respond ONLY with the hex color code, nothing else.";

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(20)
                .temperature(0.7)
                .build();

            String response = openAiService.createChatCompletion(completionRequest)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent()
                .trim();

            // Validate hex color format
            if (response.matches("^#[0-9A-Fa-f]{6}$")) {
                log.info("Successfully suggested color: {} for project: {}", response, projectName);
                return response;
            } else {
                log.warn("Invalid color format from AI: {}, using default", response);
                return "#1473E6"; // Default blue color
            }

        } catch (Exception e) {
            log.error("Error suggesting color: {}", e.getMessage(), e);
            return "#1473E6"; // Return default color on error
        }
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }
}
