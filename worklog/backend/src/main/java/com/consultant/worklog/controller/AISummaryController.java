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
}
