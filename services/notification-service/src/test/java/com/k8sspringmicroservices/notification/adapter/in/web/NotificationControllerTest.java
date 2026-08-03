package com.k8sspringmicroservices.notification.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.k8sspringmicroservices.common.security.AuthenticatedUser;
import com.k8sspringmicroservices.notification.application.port.in.NotificationUseCase;
import com.k8sspringmicroservices.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

  @TestConfiguration
  static class AuthenticationPrincipalResolverConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
      resolvers.add(new AuthenticationPrincipalArgumentResolver());
    }
  }

  @Autowired private MockMvc mockMvc;
  @MockitoBean private NotificationUseCase notificationUseCase;

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
  void listMyNotifications_returns200() throws Exception {
    authenticateAs("u-1", "USER");
    Notification notification =
        new Notification("n-1", "task-1", "u-1", "Task created", Instant.now());
    when(notificationUseCase.listByOwner("u-1")).thenReturn(List.of(notification));

    mockMvc
        .perform(get("/notifications/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(1)))
        .andExpect(jsonPath("$.data[0].id", is("n-1")))
        .andExpect(jsonPath("$.data[0].ownerId", is("u-1")));
  }

  @Test
  void listMyNotifications_returns200_withEmptyList() throws Exception {
    authenticateAs("u-2", "USER");
    when(notificationUseCase.listByOwner("u-2")).thenReturn(List.of());

    mockMvc
        .perform(get("/notifications/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(0)));
  }
}
