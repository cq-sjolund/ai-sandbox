package com.consultant.worklog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDTO {

    private Long id;

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "Color code is required")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color code must be a valid hex color (e.g., #FF5733)")
    private String colorCode;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
