package com.consultant.worklog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicsTimeEntryDTO {

    @JsonProperty("msdyn_timeentryid")
    private String timeEntryId;

    @JsonProperty("msdyn_date")
    private String date; // ISO date format

    @JsonProperty("msdyn_duration")
    private Integer duration; // Duration in minutes

    @JsonProperty("msdyn_description")
    private String description;

    @JsonProperty("_msdyn_project_value")
    private String projectId;

    @JsonProperty("msdyn_projectName")
    private String projectName;

    @JsonProperty("_msdyn_bookableresource_value")
    private String bookableResourceId;

    @JsonProperty("msdyn_type")
    private Integer type; // 192350000 = Work

    @JsonProperty("msdyn_entrystatus")
    private Integer entryStatus; // 192350000 = Draft
}
