package com.consultant.worklog.controller;

import com.consultant.worklog.service.DynamicsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dynamics")
@RequiredArgsConstructor
@Slf4j
public class DynamicsController {

    private final DynamicsService dynamicsService;

    @PostMapping("/analyze-mappings")
    public ResponseEntity<Map<String, Object>> analyzeMappings(@RequestBody Map<String, Object> request) {
        log.info("POST /api/dynamics/analyze-mappings - Analyzing project mappings with AI");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> entries = (java.util.List<Map<String, Object>>) request.get("entries");

        if (entries == null || entries.isEmpty()) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", false);
            result.put("message", "No entries provided");
            return ResponseEntity.ok(result);
        }

        Map<String, Object> result = dynamicsService.analyzeProjectMappings(entries);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importEntries(@RequestBody Map<String, Object> request) {
        log.info("POST /api/dynamics/import - Importing entries from frontend");
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> entries = (java.util.List<Map<String, Object>>) request.get("entries");
        @SuppressWarnings("unchecked")
        Map<String, String> projectMappings = (Map<String, String>) request.get("projectMappings");

        if (entries == null || entries.isEmpty()) {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", false);
            result.put("message", "No entries provided");
            result.put("imported", 0);
            result.put("skipped", 0);
            return ResponseEntity.ok(result);
        }

        if (projectMappings == null) {
            projectMappings = new java.util.HashMap<>();
        }

        Map<String, Object> result = dynamicsService.importEntriesFromFrontend(entries, projectMappings);
        return ResponseEntity.ok(result);
    }
}
