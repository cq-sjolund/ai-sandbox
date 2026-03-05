package com.consultant.worklog.controller;

import com.consultant.worklog.dto.AISummaryRequestDTO;
import com.consultant.worklog.service.OpenAIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AISummaryController {

    private final OpenAIService openAIService;

    @PostMapping("/summary")
    public ResponseEntity<Map<String, String>> generateSummary(
        @Valid @RequestBody AISummaryRequestDTO request
    ) {
        log.debug("POST /api/ai/summary - Generating AI summary");

        try {
            String summary = openAIService.generateSummary(request);

            Map<String, String> response = new HashMap<>();
            response.put("summary", summary);
            response.put("dateRangeStart", request.getDateRangeStart().toString());
            response.put("dateRangeEnd", request.getDateRangeEnd().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating AI summary: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate AI summary");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @PostMapping("/suggest-color")
    public ResponseEntity<Map<String, String>> suggestColor(
        @RequestBody Map<String, String> request
    ) {
        String projectName = request.get("projectName");
        log.debug("POST /api/ai/suggest-color - Suggesting color for project: {}", projectName);

        try {
            String colorCode = openAIService.suggestProjectColor(projectName);

            Map<String, String> response = new HashMap<>();
            response.put("colorCode", colorCode);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error suggesting color: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to suggest color");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("colorCode", "#1473E6"); // Return default color even on error
            return ResponseEntity.ok(errorResponse);
        }
    }

    @PostMapping("/complete-description")
    public ResponseEntity<Map<String, String>> completeDescription(
        @RequestBody Map<String, String> request
    ) {
        String currentText = request.getOrDefault("currentText", "");
        String summary = request.getOrDefault("summary", "");
        String projectName = request.getOrDefault("projectName", "");

        log.debug("POST /api/ai/complete-description - Completing description");

        try {
            String completion = openAIService.completeDescription(currentText, summary, projectName);

            Map<String, String> response = new HashMap<>();
            response.put("completion", completion);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error completing description: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("completion", "");
            return ResponseEntity.ok(response);
        }
    }
}
