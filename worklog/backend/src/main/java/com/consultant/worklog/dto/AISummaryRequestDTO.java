package com.consultant.worklog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISummaryRequestDTO {

    @NotNull(message = "Start date is required")
    private LocalDate dateRangeStart;

    @NotNull(message = "End date is required")
    private LocalDate dateRangeEnd;

    private List<Long> projectIds;

    private String customPrompt;
}
