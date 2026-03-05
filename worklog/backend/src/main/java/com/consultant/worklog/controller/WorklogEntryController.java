package com.consultant.worklog.controller;

import com.consultant.worklog.dto.WorklogEntryDTO;
import com.consultant.worklog.service.WorklogEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
@Slf4j
public class WorklogEntryController {

    private final WorklogEntryService worklogEntryService;

    @GetMapping
    public ResponseEntity<List<WorklogEntryDTO>> getAllEntries() {
        log.debug("GET /api/entries - Fetching all entries");
        List<WorklogEntryDTO> entries = worklogEntryService.getAllEntries();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorklogEntryDTO> getEntryById(@PathVariable Long id) {
        log.debug("GET /api/entries/{} - Fetching entry by id", id);
        WorklogEntryDTO entry = worklogEntryService.getEntryById(id);
        return ResponseEntity.ok(entry);
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<WorklogEntryDTO>> getEntriesByDate(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.debug("GET /api/entries/date/{} - Fetching entries by date", date);
        List<WorklogEntryDTO> entries = worklogEntryService.getEntriesByDate(date);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/range")
    public ResponseEntity<List<WorklogEntryDTO>> getEntriesByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        log.debug("GET /api/entries/range?start={}&end={} - Fetching entries by date range", start, end);
        List<WorklogEntryDTO> entries = worklogEntryService.getEntriesByDateRange(start, end);
        return ResponseEntity.ok(entries);
    }

    @PostMapping
    public ResponseEntity<WorklogEntryDTO> createEntry(@Valid @RequestBody WorklogEntryDTO entryDTO) {
        log.debug("POST /api/entries - Creating new entry");
        WorklogEntryDTO createdEntry = worklogEntryService.createEntry(entryDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEntry);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorklogEntryDTO> updateEntry(
        @PathVariable Long id,
        @Valid @RequestBody WorklogEntryDTO entryDTO
    ) {
        log.debug("PUT /api/entries/{} - Updating entry", id);
        WorklogEntryDTO updatedEntry = worklogEntryService.updateEntry(id, entryDTO);
        return ResponseEntity.ok(updatedEntry);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        log.debug("DELETE /api/entries/{} - Deleting entry", id);
        worklogEntryService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }
}
