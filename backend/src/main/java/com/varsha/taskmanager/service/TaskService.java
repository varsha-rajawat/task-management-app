package com.varsha.taskmanager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varsha.taskmanager.dto.CreateTaskRequest;
import com.varsha.taskmanager.dto.TaskResponse;
import com.varsha.taskmanager.dto.UpdateTaskRequest;
import com.varsha.taskmanager.exception.AppException;
import com.varsha.taskmanager.exception.ResourceNotFoundException;
import com.varsha.taskmanager.model.Role;
import com.varsha.taskmanager.model.Task;
import com.varsha.taskmanager.model.TaskPriority;
import com.varsha.taskmanager.model.TaskStatus;
import com.varsha.taskmanager.model.User;
import com.varsha.taskmanager.repository.TaskRepository;
import com.varsha.taskmanager.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // Create a new task
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, User creator) {
        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getAssigneeId()));
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .createdBy(creator)
                .assignedTo(assignee)
                .dueDate(request.getDueDate())
                .build();

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    // List (paginated + filtered)
    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasks(TaskStatus status, TaskPriority priority, UUID assigneeId, String search,
            Pageable pageable) {

        Specification<Task> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assigneeId));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return taskRepository.findAll(spec, pageable).map(TaskResponse::fromEntity);
    }

    // ── GET BY ID ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        return TaskResponse.fromEntity(task);
    }

    // ── UPDATE ─────────────────────────────────────────────────
    @Transactional
    public TaskResponse updateTask(UUID id, UpdateTaskRequest request, User currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        // Only the creator or an ADMIN can edit a task
        boolean isOwner = task.getCreatedBy() != null
                && task.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AppException("You don't have permission to edit this task", HttpStatus.FORBIDDEN);
        }

        // Apply only non-null fields (partial update)
        if (request.getTitle() != null)
            task.setTitle(request.getTitle());
        if (request.getDescription() != null)
            task.setDescription(request.getDescription());
        if (request.getStatus() != null)
            task.setStatus(request.getStatus());
        if (request.getPriority() != null)
            task.setPriority(request.getPriority());
        if (request.getDueDate() != null)
            task.setDueDate(request.getDueDate());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getAssigneeId()));
            task.setAssignedTo(assignee);
        }

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    // ── PATCH STATUS ───────────────────────────────────────────
    @Transactional
    public TaskResponse updateStatus(UUID id, TaskStatus newStatus, User currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        task.setStatus(newStatus);
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    // ── DELETE ─────────────────────────────────────────────────
    @Transactional
    public void deleteTask(UUID id, User currentUser) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        boolean isOwner = task.getCreatedBy() != null
                && task.getCreatedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AppException("You don't have permission to delete this task", HttpStatus.FORBIDDEN);
        }

        taskRepository.delete(task);
    }

    // ── DASHBOARD COUNTS ───────────────────────────────────────
    public record DashboardStats(long total, long myTasks, long overdue, long completedToday) {
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats(User currentUser) {
        long total = taskRepository.count();
        long myTasks = taskRepository.countByAssignedTo_IdAndStatus(
                currentUser.getId(), TaskStatus.TODO)
                + taskRepository.countByAssignedTo_IdAndStatus(
                        currentUser.getId(), TaskStatus.IN_PROGRESS);
        long overdue = taskRepository.countOverdueTasks();
        long completedToday = taskRepository.countByStatus(TaskStatus.DONE);

        return new DashboardStats(total, myTasks, overdue, completedToday);
    }

}
