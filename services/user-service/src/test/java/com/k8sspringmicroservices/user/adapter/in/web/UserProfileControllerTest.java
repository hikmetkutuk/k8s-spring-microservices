package com.k8sspringmicroservices.user.adapter.in.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
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
import com.k8sspringmicroservices.user.adapter.in.web.dto.CreateProfileRequest;
import com.k8sspringmicroservices.user.adapter.in.web.dto.UpdateProfileRequest;
import com.k8sspringmicroservices.user.application.port.in.UserProfileUseCase;
import com.k8sspringmicroservices.user.domain.UserProfile;
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

// Bu WebMvcTest slice'i icin RequestMappingHandlerAdapter'in argumentResolvers listesinde
// Spring Security'nin AuthenticationPrincipalArgumentResolver'i kayitli degil (slice, tam
// SecurityFilterChain/WebMvcSecurityConfiguration wiring'ini yuklemiyor) -- bu yuzden
// @AuthenticationPrincipal parametresi MVC'nin son care ServletModelAttributeMethodProcessor'una
// dusuyor ve AuthenticatedUser record'unu bos/path-variable kaynaklardan insa ediyor. Resolver'i
// burada manuel ekliyoruz; SecurityContextHolder'i da MockMvc istegi ayni thread'de senkron
// calistigi icin dogrudan set ediyoruz.
@WebMvcTest(UserProfileController.class)
@Import(GlobalExceptionHandler.class)
class UserProfileControllerTest {

  @TestConfiguration
  static class AuthenticationPrincipalResolverConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
      resolvers.add(new AuthenticationPrincipalArgumentResolver());
    }
  }

  @Autowired private MockMvc mockMvc;
  @MockitoBean private UserProfileUseCase userProfileUseCase;

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

  @Test
  void createMyProfile_returns201() throws Exception {
    authenticateAs("u-1", "USER");
    UserProfile profile =
        new UserProfile("u-1", "Alice", "bio", "avatar.png", Instant.now(), Instant.now());
    when(userProfileUseCase.createProfile("u-1", "Alice", "bio", "avatar.png")).thenReturn(profile);

    mockMvc
        .perform(
            post("/users/me")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new CreateProfileRequest("Alice", "bio", "avatar.png"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.userId", is("u-1")))
        .andExpect(jsonPath("$.data.displayName", is("Alice")));
  }

  @Test
  void createMyProfile_returns400_whenDisplayNameMissing() throws Exception {
    authenticateAs("u-1", "USER");

    mockMvc
        .perform(
            post("/users/me")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(new CreateProfileRequest("", "bio", null))))
        .andExpect(status().isBadRequest());

    verify(userProfileUseCase, never()).createProfile(any(), any(), any(), any());
  }

  @Test
  void getMyProfile_returns200() throws Exception {
    authenticateAs("u-1", "USER");
    UserProfile profile =
        new UserProfile("u-1", "Alice", "bio", "avatar.png", Instant.now(), Instant.now());
    when(userProfileUseCase.getProfile("u-1")).thenReturn(profile);

    mockMvc
        .perform(get("/users/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId", is("u-1")));
  }

  @Test
  void updateMyProfile_returns200() throws Exception {
    authenticateAs("u-1", "USER");
    UserProfile updated =
        new UserProfile("u-1", "Alice2", "new bio", "avatar2.png", Instant.now(), Instant.now());
    when(userProfileUseCase.updateProfile("u-1", "Alice2", "new bio", "avatar2.png"))
        .thenReturn(updated);

    mockMvc
        .perform(
            put("/users/me")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateProfileRequest("Alice2", "new bio", "avatar2.png"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.displayName", is("Alice2")));
  }

  @Test
  void deleteMyProfile_returns204() throws Exception {
    authenticateAs("u-1", "USER");

    mockMvc.perform(delete("/users/me")).andExpect(status().isNoContent());

    verify(userProfileUseCase).deleteProfile("u-1");
  }

  @Test
  void getProfile_returns200_whenRequestingOwnProfile() throws Exception {
    authenticateAs("u-1", "USER");
    UserProfile profile =
        new UserProfile("u-1", "Alice", "bio", "avatar.png", Instant.now(), Instant.now());
    when(userProfileUseCase.getProfile("u-1")).thenReturn(profile);

    mockMvc.perform(get("/users/u-1")).andExpect(status().isOk());
  }

  @Test
  void getProfile_returns200_whenAdminRequestingOtherProfile() throws Exception {
    authenticateAs("admin-1", "ADMIN");
    UserProfile profile =
        new UserProfile("u-2", "Bob", "bio", "avatar.png", Instant.now(), Instant.now());
    when(userProfileUseCase.getProfile("u-2")).thenReturn(profile);

    mockMvc.perform(get("/users/u-2")).andExpect(status().isOk());
  }

  @Test
  void getProfile_returns403_whenNonAdminRequestingOtherProfile() throws Exception {
    authenticateAs("u-1", "USER");

    mockMvc.perform(get("/users/u-2")).andExpect(status().isForbidden());

    verify(userProfileUseCase, never()).getProfile("u-2");
  }
}
