package com.consultant.worklog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicsConfigDTO {
    private Long id;

    @NotBlank(message = "Organization URL is required")
    private String organizationUrl;

    @NotBlank(message = "Access token is required")
    private String accessToken;

    private String bookableResourceId;
    private boolean enabled;
}
