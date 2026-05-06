package com.varsha.taskmanager.dto;

import com.varsha.taskmanager.model.TaskPriority;
import com.varsha.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * All fields are optional — only non-null fields are applied.
 * This is a "partial update" pattern (PATCH semantics without @Patch).
 * Service code checks for null before overwriting.
 */
@Data
public class UpdateTaskRequest {

    @Size(max = 200)
    private String title;

    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private UUID assigneeId;
    private LocalDate dueDate;
}
