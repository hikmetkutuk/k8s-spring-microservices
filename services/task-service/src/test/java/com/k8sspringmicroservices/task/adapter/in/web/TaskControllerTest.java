package com.k8sspringmicroservices.task.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.k8sspringmicroservices.common.exception.GlobalExceptionHandler;
import com.k8sspringmicroservices.common.security.AuthenticatedUser;
import com.k8sspringmicroservices.task.adapter.in.web.dto.CreateTaskRequest;
import com.k8sspringmicroservices.task.adapter.in.web.dto.UpdateTaskRequest;
import com.k8sspringmicroservices.task.application.port.in.TaskUseCase;
import com.k8sspringmicroservices.task.domain.Task;
import com.k8sspringmicroservices.task.domain.TaskStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Bkz. UserProfileControllerTest: bu WebMvcTest slice'inda AuthenticationPrincipalArgumentResolver
// varsayilan olarak kayitli degil, bu yuzden manuel ekliyoruz; SecurityContextHolder de MockMvc
// istegiyle ayni thread'de senkron calistigi icin dogrudan set ediliyor.
@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

  @TestConfiguration
  static class AuthenticationPrincipalResolverConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
      resolvers.add(new AuthenticationPrincipalArgumentResolver());
    }
  }

  @Autowired private MockMvc mockMvc;
  @MockitoBean private TaskUseCase taskUseCase;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static void authenticateAs(String userId, String... roles) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(userId, List.of(roles)), null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private static Task sampleTask(String id, String ownerId) {
    return new Task(
        id,
        ownerId,
        "item-1",
        "Title",
        "desc",
        2,
        TaskStatus.PENDING,
        Instant.now(),
        Instant.now());
  }

  @Test
  void createMyTask_returns201() throws Exception {
    authenticateAs("u-1", "USER");
    when(taskUseCase.create(anyString(), anyString(), anyString(), any(), anyInt()))
        .thenReturn(sampleTask("task-1", "u-1"));

    mockMvc
        .perform(
            post("/tasks/me")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new CreateTaskRequest("item-1", "Title", "desc", 2))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id", is("task-1")))
        .andExpect(jsonPath("$.data.ownerId", is("u-1")));

    verify(taskUseCase).create("u-1", "item-1", "Title", "desc", 2);
  }

  @Test
  void createMyTask_returns400_whenTitleMissing() throws Exception {
    authenticateAs("u-1", "USER");

    mockMvc
        .perform(
            post("/tasks/me")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new CreateTaskRequest("item-1", "", "desc", 2))))
        .andExpect(status().isBadRequest());

    verify(taskUseCase, never()).create(any(), any(), any(), any(), anyInt());
  }

  @Test
  void listMyTasks_returns200() throws Exception {
    authenticateAs("u-1", "USER");
    when(taskUseCase.listByOwner("u-1")).thenReturn(List.of(sampleTask("task-1", "u-1")));

    mockMvc
        .perform(get("/tasks/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(1)))
        .andExpect(jsonPath("$.data[0].id", is("task-1")));
  }

  @Test
  void get_returns200_whenOwner() throws Exception {
    authenticateAs("u-1", "USER");
    when(taskUseCase.get("task-1")).thenReturn(sampleTask("task-1", "u-1"));

    mockMvc
        .perform(get("/tasks/task-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id", is("task-1")));
  }

  @Test
  void get_returns200_whenAdmin() throws Exception {
    authenticateAs("admin-1", "ADMIN");
    when(taskUseCase.get("task-1")).thenReturn(sampleTask("task-1", "u-1"));

    mockMvc.perform(get("/tasks/task-1")).andExpect(status().isOk());
  }

  @Test
  void get_returns403_whenNotOwnerOrAdmin() throws Exception {
    authenticateAs("u-2", "USER");
    when(taskUseCase.get("task-1")).thenReturn(sampleTask("task-1", "u-1"));

    mockMvc.perform(get("/tasks/task-1")).andExpect(status().isForbidden());
  }

  @Test
  void update_returns200_whenOwner() throws Exception {
    authenticateAs("u-1", "USER");
    when(taskUseCase.get("task-1")).thenReturn(sampleTask("task-1", "u-1"));
    when(taskUseCase.update(anyString(), anyString(), any(), anyInt()))
        .thenReturn(sampleTask("task-1", "u-1"));

    mockMvc
        .perform(
            put("/tasks/task-1")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(new UpdateTaskRequest("New title", "desc", 3))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id", is("task-1")));
  }

  @Test
  void update_returns403_whenNotOwnerOrAdmin() throws Exception {
    authenticateAs("u-2", "USER");
    when(taskUseCase.get("task-1")).thenReturn(sampleTask("task-1", "u-1"));

    mockMvc
        .perform(
            put("/tasks/task-1")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(new UpdateTaskRequest("New title", "desc", 3))))
        .andExpect(status().isForbidden());

    verify(taskUseCase, never()).update(any(), any(), any(), anyInt());
  }

  @Test
  void delete_returns204_whenOwner() throws Exception {
    authenticateAs("u-1", "USER");
    when(taskUseCase.get("task-1")).thenReturn(sampleTask("task-1", "u-1"));

    mockMvc.perform(delete("/tasks/task-1")).andExpect(status().isNoContent());

    verify(taskUseCase).delete("task-1");
  }

  @Test
  void delete_returns403_whenNotOwnerOrAdmin() throws Exception {
    authenticateAs("u-2", "USER");
    when(taskUseCase.get("task-1")).thenReturn(sampleTask("task-1", "u-1"));

    mockMvc.perform(delete("/tasks/task-1")).andExpect(status().isForbidden());

    verify(taskUseCase, never()).delete(any());
  }
}
