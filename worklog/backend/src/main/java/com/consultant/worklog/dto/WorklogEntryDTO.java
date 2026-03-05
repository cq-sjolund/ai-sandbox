package com.consultant.worklog.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorklogEntryDTO {

    private Long id;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    @NotBlank(message = "Summary is required")
    @Size(max = 255, message = "Summary must not exceed 255 characters")
    private String summary;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Hours is required")
    @DecimalMin(value = "0.1", inclusive = true, message = "Hours must be at least 0.1")
    @DecimalMax(value = "24.0", inclusive = true, message = "Hours must not exceed 24.0")
    private BigDecimal hours;

    @NotNull(message = "Project ID is required")
    private Long projectId;

    private ProjectDTO project;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
