package com.k8sspringmicroservices.auth.adapter.in.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.k8sspringmicroservices.auth.adapter.in.web.dto.LoginRequest;
import com.k8sspringmicroservices.auth.adapter.in.web.dto.RefreshRequest;
import com.k8sspringmicroservices.auth.adapter.in.web.dto.RegisterRequest;
import com.k8sspringmicroservices.auth.application.port.in.AuthUseCase;
import com.k8sspringmicroservices.common.exception.ApplicationException;
import com.k8sspringmicroservices.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AuthUseCase authUseCase;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void register_returns201_withTokenPair() throws Exception {
    when(authUseCase.register("alice", "alice@example.com", "Sup3rSecret!"))
        .thenReturn(new AuthUseCase.TokenPair("access-token", "refresh-token", 900L));

    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("alice", "alice@example.com", "Sup3rSecret!"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.accessToken", is("access-token")))
        .andExpect(jsonPath("$.data.refreshToken", is("refresh-token")))
        .andExpect(jsonPath("$.data.tokenType", is("Bearer")));
  }

  @Test
  void register_returns400_whenRequestInvalid() throws Exception {
    mockMvc
        .perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("ab", "not-an-email", "short"))))
        .andExpect(status().isBadRequest());

    verify(authUseCase, never()).register(any(), any(), any());
  }

  @Test
  void login_returns200_withTokenPair() throws Exception {
    when(authUseCase.login("alice", "Sup3rSecret!"))
        .thenReturn(new AuthUseCase.TokenPair("access-token", "refresh-token", 900L));

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new LoginRequest("alice", "Sup3rSecret!"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accessToken", is("access-token")));
  }

  @Test
  void login_returns401_whenCredentialsInvalid() throws Exception {
    when(authUseCase.login("alice", "wrong"))
        .thenThrow(
            new ApplicationException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

    mockMvc
        .perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("alice", "wrong"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message", is("Invalid username or password")));
  }

  @Test
  void logout_returns204() throws Exception {
    mockMvc
        .perform(
            post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest("refresh-token"))))
        .andExpect(status().isNoContent());

    verify(authUseCase).logout("refresh-token");
  }
}
