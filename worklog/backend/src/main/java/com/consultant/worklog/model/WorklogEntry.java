package com.consultant.worklog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "worklog_entries", indexes = {
    @Index(name = "idx_entries_date", columnList = "entry_date"),
    @Index(name = "idx_entries_project", columnList = "project_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorklogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Entry date is required")
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @NotBlank(message = "Summary is required")
    @Size(max = 255, message = "Summary must not exceed 255 characters")
    @Column(nullable = false)
    private String summary;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Hours is required")
    @DecimalMin(value = "0.1", inclusive = true, message = "Hours must be at least 0.1")
    @DecimalMax(value = "24.0", inclusive = true, message = "Hours must not exceed 24.0")
    @Digits(integer = 3, fraction = 2, message = "Hours must have at most 3 integer digits and 2 decimal places")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal hours;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    @NotNull(message = "Project is required")
    private Project project;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
