package com.k8sspringmicroservices.task.adapter.in.web;

import com.k8sspringmicroservices.common.dto.ApiResponse;
import com.k8sspringmicroservices.common.exception.ApplicationException;
import com.k8sspringmicroservices.common.security.AuthenticatedUser;
import com.k8sspringmicroservices.task.adapter.in.web.dto.CreateTaskRequest;
import com.k8sspringmicroservices.task.adapter.in.web.dto.TaskResponse;
import com.k8sspringmicroservices.task.adapter.in.web.dto.UpdateTaskRequest;
import com.k8sspringmicroservices.task.application.port.in.TaskUseCase;
import com.k8sspringmicroservices.task.domain.Task;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {

  private final TaskUseCase taskUseCase;

  public TaskController(TaskUseCase taskUseCase) {
    this.taskUseCase = taskUseCase;
  }

  @PostMapping("/me")
  public ResponseEntity<ApiResponse<TaskResponse>> createMyTask(
      @AuthenticationPrincipal AuthenticatedUser currentUser,
      @Valid @RequestBody CreateTaskRequest request) {
    Task task =
        taskUseCase.create(
            currentUser.userId(),
            request.catalogItemId(),
            request.title(),
            request.description(),
            request.quantity());
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(TaskResponse.from(task)));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<List<TaskResponse>>> listMyTasks(
      @AuthenticationPrincipal AuthenticatedUser currentUser) {
    List<TaskResponse> tasks =
        taskUseCase.listByOwner(currentUser.userId()).stream().map(TaskResponse::from).toList();
    return ResponseEntity.ok(ApiResponse.of(tasks));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<TaskResponse>> get(
      @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable String id) {
    Task task = taskUseCase.get(id);
    requireOwnerOrAdmin(currentUser, task);
    return ResponseEntity.ok(ApiResponse.of(TaskResponse.from(task)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<TaskResponse>> update(
      @AuthenticationPrincipal AuthenticatedUser currentUser,
      @PathVariable String id,
      @Valid @RequestBody UpdateTaskRequest request) {
    requireOwnerOrAdmin(currentUser, taskUseCase.get(id));
    Task updated =
        taskUseCase.update(id, request.title(), request.description(), request.quantity());
    return ResponseEntity.ok(ApiResponse.of(TaskResponse.from(updated)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable String id) {
    requireOwnerOrAdmin(currentUser, taskUseCase.get(id));
    taskUseCase.delete(id);
    return ResponseEntity.noContent().build();
  }

  private void requireOwnerOrAdmin(AuthenticatedUser currentUser, Task task) {
    if (!currentUser.userId().equals(task.ownerId()) && !currentUser.hasRole("ADMIN")) {
      throw new ApplicationException(HttpStatus.FORBIDDEN, "Not allowed to access this task");
    }
  }
}
