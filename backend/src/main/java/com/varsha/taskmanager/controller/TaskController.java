package com.varsha.taskmanager.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.varsha.taskmanager.dto.CreateTaskRequest;
import com.varsha.taskmanager.dto.TaskResponse;
import com.varsha.taskmanager.dto.UpdateTaskRequest;
import com.varsha.taskmanager.model.TaskPriority;
import com.varsha.taskmanager.model.TaskStatus;
import com.varsha.taskmanager.model.User;
import com.varsha.taskmanager.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;
    // private final AiService aiService;

    // ── CREATE ─────────────────────────────────────────────────
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new task")
    public TaskResponse createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {
        return taskService.createTask(request, currentUser);
    }

    // ── LIST ───────────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "List tasks with optional filters and pagination")
    public Page<TaskResponse> getTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return taskService.getTasks(status, priority, assigneeId, search, pageable);
    }

    // ── GET BY ID ──────────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID")
    public TaskResponse getTask(@PathVariable UUID id) {
        return taskService.getTaskById(id);
    }

    // ── UPDATE ─────────────────────────────────────────────────
    @PutMapping("/{id}")
    @Operation(summary = "Update a task (creator or ADMIN only)")
    public TaskResponse updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal User currentUser) {
        return taskService.updateTask(id, request, currentUser);
    }

    // ── PATCH STATUS ───────────────────────────────────────────
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status only")
    public TaskResponse updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        TaskStatus newStatus = TaskStatus.valueOf(body.get("status").toUpperCase());
        return taskService.updateStatus(id, newStatus, currentUser);
    }

    // ── DELETE ─────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a task (creator or ADMIN only)")
    public void deleteTask(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        taskService.deleteTask(id, currentUser);
    }

    // ── DASHBOARD STATS ────────────────────────────────────────
    @GetMapping("/dashboard-stats")
    @Operation(summary = "Get counts for the dashboard summary cards")
    public TaskService.DashboardStats getDashboardStats(@AuthenticationPrincipal User currentUser) {
        return taskService.getDashboardStats(currentUser);
    }

}
