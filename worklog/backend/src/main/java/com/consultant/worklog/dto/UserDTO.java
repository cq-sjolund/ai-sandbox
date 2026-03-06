package com.consultant.worklog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String role;
    private boolean enabled;
    private String bookableResourceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
