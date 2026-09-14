package com.k8sspringmicroservices.auth.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.k8sspringmicroservices.auth.domain.User;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserPersistenceAdapterIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void flywayProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired private UserJpaRepository jpaRepository;

  private UserPersistenceAdapter adapter;

  @Test
  void savesAndReloadsUser_roundTripsAllFields() {
    adapter = new UserPersistenceAdapter(jpaRepository, new UserPersistenceMapper());

    User user =
        new User(
            UUID.randomUUID().toString(),
            "bob",
            "bob@example.com",
            "hashed-secret",
            Set.of("USER", "ADMIN"),
            true);

    adapter.save(user);

    Optional<User> reloaded = adapter.findByUsername("bob");

    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getId()).isEqualTo(user.getId());
    assertThat(reloaded.get().getEmail()).isEqualTo("bob@example.com");
    assertThat(reloaded.get().getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");
    assertThat(reloaded.get().isEnabled()).isTrue();
    assertThat(adapter.existsByUsername("bob")).isTrue();
    assertThat(adapter.existsByUsername("nonexistent")).isFalse();
  }

  @Test
  void findById_returnsEmpty_whenUserDoesNotExist() {
    adapter = new UserPersistenceAdapter(jpaRepository, new UserPersistenceMapper());

    assertThat(adapter.findById(UUID.randomUUID().toString())).isEmpty();
  }
}
