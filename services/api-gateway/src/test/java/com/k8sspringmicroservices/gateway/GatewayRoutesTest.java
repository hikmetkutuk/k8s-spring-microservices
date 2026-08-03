package com.k8sspringmicroservices.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.k8sspringmicroservices.gateway.config.GatewaySecurityProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutesTest {

  @LocalServerPort private int port;

  @Autowired private GatewaySecurityProperties securityProperties;

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void healthEndpoint_isAccessibleWithoutToken() {
    // Redis excluded in test profile → health aggregate DOWN (503).
    // Critical assertion: public endpoint, no 401 from JWT filter.
    webTestClient
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus()
        .value(status -> assertThat(status).isNotEqualTo(401));
  }

  @Test
  void publicPath_authRegister_doesNotRequireToken() {
    webTestClient
        .get()
        .uri("/auth/register")
        .exchange()
        .expectStatus()
        .value(status -> assertThat(status).isNotEqualTo(401));
  }

  @Test
  void protectedPath_tasksMe_returnsUnauthorizedWithoutToken() {
    webTestClient.get().uri("/tasks/me").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void protectedPath_tasksMe_returnsUnauthorizedWithInvalidToken() {
    webTestClient
        .get()
        .uri("/tasks/me")
        .header("Authorization", "Bearer invalid-token-value")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void protectedPath_tasksMe_returnsUnauthorizedWithMalformedToken() {
    webTestClient
        .get()
        .uri("/tasks/me")
        .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.invalid")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void swaggerUi_doesNotRequireToken() {
    webTestClient
        .get()
        .uri("/swagger-ui/index.html")
        .exchange()
        .expectStatus()
        .value(status -> assertThat(status).isNotEqualTo(401));
  }

  @Test
  void authDocs_doesNotRequireToken() {
    webTestClient
        .get()
        .uri("/auth-docs/v3/api-docs")
        .exchange()
        .expectStatus()
        .value(status -> assertThat(status).isNotEqualTo(401));
  }

  @Test
  void allConfiguredPublicPaths_areRecognized() {
    List<String> publicPaths = securityProperties.getPublicPaths();
    assertThat(publicPaths).isNotEmpty();
    assertThat(publicPaths).contains("/auth/register", "/auth/login", "/auth/refresh");
    assertThat(publicPaths).contains("/actuator/health");
  }

  @Test
  void protectedPath_usersMe_returnsUnauthorizedWithoutToken() {
    webTestClient.get().uri("/users/me").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void protectedPath_catalogItems_returnsUnauthorizedWithoutToken() {
    webTestClient.get().uri("/catalog-items").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void protectedPath_notificationsMe_returnsUnauthorizedWithoutToken() {
    webTestClient.get().uri("/notifications/me").exchange().expectStatus().isUnauthorized();
  }
}
