package com.consultant.worklog.service;

import com.consultant.worklog.dto.AISummaryRequestDTO;
import com.consultant.worklog.model.WorklogEntry;
import com.consultant.worklog.repository.WorklogEntryRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
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
            log.info("=== AI API CALL: Generate Summary ===");
            log.info("OpenAI Request URL: POST https://api.openai.com/v1/chat/completions");
            log.info("Model: {}", model);
            log.info("Max Tokens: 1000, Temperature: 0.7");

            String systemMessage = "You are a professional consultant assistant who helps summarize worklog entries.";
            log.info("System Message: {}", systemMessage);
            log.info("User Prompt: {}", prompt);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(1000)
                .temperature(0.7)
                .build();

            ChatCompletionResult result = openAiService.createChatCompletion(completionRequest);
            String response = result.getChoices().get(0).getMessage().getContent();

            // Log token usage
            if (result.getUsage() != null) {
                log.info("TOKEN USAGE - Prompt: {} | Completion: {} | Total: {} tokens",
                    result.getUsage().getPromptTokens(),
                    result.getUsage().getCompletionTokens(),
                    result.getUsage().getTotalTokens());
            }

            log.info("AI Response length: {} characters", response.length());
            log.debug("AI Response (first 200 chars): {}", response.substring(0, Math.min(200, response.length())));
            log.info("=== AI API CALL COMPLETED ===");
            return response;

        } catch (Exception e) {
            log.error("=== AI API CALL FAILED ===");
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate AI summary: " + e.getMessage(), e);
        }
    }

    public String suggestProjectColor(String projectName) {
        try {
            log.info("=== AI API CALL: Suggest Project Color ===");
            log.info("OpenAI Request URL: POST https://api.openai.com/v1/chat/completions");
            log.info("Model: {}", model);
            log.info("Project Name: {}", projectName);
            log.info("Max Tokens: 20, Temperature: 0.7");

            String prompt = "You are a color expert. Based on the project name \"" + projectName +
                "\", suggest an appropriate hex color code (e.g., #FF5733). " +
                "Consider common color associations (e.g., Marketing=orange/red, Development=blue, Sales=green, HR=purple). " +
                "Respond ONLY with the hex color code, nothing else.";

            log.info("User Prompt: {}", prompt);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(20)
                .temperature(0.7)
                .build();

            ChatCompletionResult result = openAiService.createChatCompletion(completionRequest);
            String response = result.getChoices().get(0).getMessage().getContent().trim();

            // Log token usage
            if (result.getUsage() != null) {
                log.info("TOKEN USAGE - Prompt: {} | Completion: {} | Total: {} tokens",
                    result.getUsage().getPromptTokens(),
                    result.getUsage().getCompletionTokens(),
                    result.getUsage().getTotalTokens());
            }

            log.info("AI Response: {}", response);

            // Validate hex color format
            if (response.matches("^#[0-9A-Fa-f]{6}$")) {
                log.info("Successfully suggested color: {} for project: {}", response, projectName);
                log.info("=== AI API CALL COMPLETED ===");
                return response;
            } else {
                log.warn("Invalid color format from AI: {}, using default", response);
                log.info("=== AI API CALL COMPLETED (with fallback) ===");
                return "#1473E6"; // Default blue color
            }

        } catch (Exception e) {
            log.error("=== AI API CALL FAILED ===");
            log.error("Error suggesting color: {}", e.getMessage(), e);
            return "#1473E6"; // Return default color on error
        }
    }

    public String completeDescription(String currentText, String summary, String projectName) {
        try {
            log.info("=== AI API CALL: Complete Description ===");
            log.info("OpenAI Request URL: POST https://api.openai.com/v1/chat/completions");
            log.info("Model: {}", model);
            log.info("Max Tokens: 100, Temperature: 0.7");

            String prompt = "You are a helpful assistant for a consultant tracking their work. " +
                "The user is writing a worklog entry description. " +
                "Project: " + projectName + "\n" +
                "Summary: " + summary + "\n" +
                "Current description text: \"" + currentText + "\"\n\n" +
                "Suggest a natural continuation or completion of the description (1-2 sentences max). " +
                "Be concise and professional. Only provide the completion text, not the entire description.";

            log.info("User Prompt: {}", prompt);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(100)
                .temperature(0.7)
                .build();

            ChatCompletionResult result = openAiService.createChatCompletion(completionRequest);
            String response = result.getChoices().get(0).getMessage().getContent().trim();

            // Log token usage
            if (result.getUsage() != null) {
                log.info("TOKEN USAGE - Prompt: {} | Completion: {} | Total: {} tokens",
                    result.getUsage().getPromptTokens(),
                    result.getUsage().getCompletionTokens(),
                    result.getUsage().getTotalTokens());
            }

            log.info("AI Response: {}", response);
            log.info("=== AI API CALL COMPLETED ===");
            return response;

        } catch (Exception e) {
            log.error("=== AI API CALL FAILED ===");
            log.error("Error completing description: {}", e.getMessage(), e);
            return ""; // Return empty string on error
        }
    }

    @Transactional(readOnly = true)
    public String answerQuestion(String question) {
        log.info("=== AI API CALL: Answer Question ===");
        log.info("OpenAI Request URL: POST https://api.openai.com/v1/chat/completions");
        log.info("Model: {}", model);
        log.info("Question: {}", question);

        try {
            // Get all worklog entries for the current user
            List<WorklogEntry> entries = worklogEntryRepository.findAll();

            if (entries.isEmpty()) {
                log.info("No worklog entries found for user");
                log.info("=== AI API CALL COMPLETED (no data) ===");
                return "I don't have any worklog data to answer your question. Please add some worklog entries first.";
            }

            log.info("Found {} worklog entries to analyze", entries.size());

            // Build context from worklog entries
            StringBuilder context = new StringBuilder();
            context.append("Here is the user's worklog data:\n\n");

            // Group entries by project
            Map<String, List<WorklogEntry>> entriesByProject = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getProject().getName()));

            for (Map.Entry<String, List<WorklogEntry>> projectEntry : entriesByProject.entrySet()) {
                String projectName = projectEntry.getKey();
                List<WorklogEntry> projectEntries = projectEntry.getValue();

                BigDecimal projectHours = projectEntries.stream()
                    .map(WorklogEntry::getHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                context.append("Project: ").append(projectName)
                       .append(" (Total: ").append(projectHours).append(" hours)\n");

                for (WorklogEntry entry : projectEntries) {
                    context.append("  - ").append(formatDate(entry.getEntryDate()))
                           .append(": ").append(entry.getHours()).append("h - ")
                           .append(entry.getSummary());
                    if (entry.getDescription() != null && !entry.getDescription().isBlank()) {
                        context.append(" (").append(entry.getDescription()).append(")");
                    }
                    context.append("\n");
                }
                context.append("\n");
            }

            // Create prompt for OpenAI
            String prompt = context.toString() + "\nUser Question: " + question +
                           "\n\nPlease answer the question based on the worklog data provided above. " +
                           "Be specific with numbers and dates where applicable. If the question cannot be " +
                           "answered with the available data, explain what information is missing.";

            log.info("Max Tokens: 500, Temperature: 0.7");

            String systemMessage = "You are a helpful assistant that analyzes worklog data and answers questions about it. " +
                "Provide clear, concise answers with specific numbers, dates, and project names.";
            log.info("System Message: {}", systemMessage);
            log.info("User Prompt (length: {} chars): {}", prompt.length(), prompt);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemMessage));
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .maxTokens(500)
                .temperature(0.7)
                .build();

            ChatCompletionResult result = openAiService.createChatCompletion(completionRequest);
            String response = result.getChoices().get(0).getMessage().getContent().trim();

            // Log token usage
            if (result.getUsage() != null) {
                log.info("TOKEN USAGE - Prompt: {} | Completion: {} | Total: {} tokens",
                    result.getUsage().getPromptTokens(),
                    result.getUsage().getCompletionTokens(),
                    result.getUsage().getTotalTokens());
            }

            log.info("AI Response length: {} characters", response.length());
            log.debug("AI Response (first 200 chars): {}", response.substring(0, Math.min(200, response.length())));
            log.info("=== AI API CALL COMPLETED ===");
            return response;

        } catch (Exception e) {
            log.error("=== AI API CALL FAILED ===");
            log.error("Error answering question: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to answer question: " + e.getMessage(), e);
        }
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }
}
