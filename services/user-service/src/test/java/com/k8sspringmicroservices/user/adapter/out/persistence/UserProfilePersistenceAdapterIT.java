package com.k8sspringmicroservices.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.k8sspringmicroservices.user.domain.UserProfile;
import java.time.Instant;
import java.util.Optional;
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
class UserProfilePersistenceAdapterIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void flywayProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired private UserProfileJpaRepository jpaRepository;

  private UserProfilePersistenceAdapter adapter;

  @Test
  void savesAndReloadsUserProfile_roundTripsAllFields() {
    adapter = new UserProfilePersistenceAdapter(jpaRepository, new UserProfileMapper());

    UserProfile profile =
        new UserProfile(
            UUID.randomUUID().toString(),
            "John Doe",
            "Software developer",
            "https://example.com/avatar.png",
            Instant.now(),
            Instant.now());

    UserProfile saved = adapter.save(profile);

    assertThat(saved).isNotNull();
    assertThat(saved.userId()).isEqualTo(profile.userId());
    assertThat(saved.displayName()).isEqualTo("John Doe");
    assertThat(saved.bio()).isEqualTo("Software developer");
    assertThat(saved.avatarUrl()).isEqualTo("https://example.com/avatar.png");

    Optional<UserProfile> reloaded = adapter.findByUserId(profile.userId());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().userId()).isEqualTo(profile.userId());
    assertThat(reloaded.get().displayName()).isEqualTo(profile.displayName());

    assertThat(adapter.existsByUserId(profile.userId())).isTrue();
  }

  @Test
  void findByUserId_returnsEmpty_whenProfileDoesNotExist() {
    adapter = new UserProfilePersistenceAdapter(jpaRepository, new UserProfileMapper());

    assertThat(adapter.findByUserId(UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  void existsByUserId_returnsFalse_whenProfileDoesNotExist() {
    adapter = new UserProfilePersistenceAdapter(jpaRepository, new UserProfileMapper());

    assertThat(adapter.existsByUserId(UUID.randomUUID().toString())).isFalse();
  }

  @Test
  void deleteByUserId_removesProfile() {
    adapter = new UserProfilePersistenceAdapter(jpaRepository, new UserProfileMapper());

    UserProfile profile =
        new UserProfile(
            UUID.randomUUID().toString(), "To Delete", null, null, Instant.now(), Instant.now());

    adapter.save(profile);
    assertThat(adapter.existsByUserId(profile.userId())).isTrue();

    adapter.deleteByUserId(profile.userId());

    assertThat(adapter.existsByUserId(profile.userId())).isFalse();
    assertThat(adapter.findByUserId(profile.userId())).isEmpty();
  }

  @Test
  void save_updatesExistingProfile_whenSameUserIdSavedAgain() {
    adapter = new UserProfilePersistenceAdapter(jpaRepository, new UserProfileMapper());

    UserProfile original =
        new UserProfile(
            UUID.randomUUID().toString(),
            "Original Name",
            "Original bio",
            null,
            Instant.now(),
            Instant.now());

    adapter.save(original);

    UserProfile updated =
        new UserProfile(
            original.userId(),
            "Updated Name",
            "Updated bio",
            "https://new-avatar.url",
            original.createdAt(),
            Instant.now());

    UserProfile result = adapter.save(updated);

    assertThat(result.displayName()).isEqualTo("Updated Name");
    assertThat(result.bio()).isEqualTo("Updated bio");
    assertThat(result.avatarUrl()).isEqualTo("https://new-avatar.url");

    Optional<UserProfile> reloaded = adapter.findByUserId(original.userId());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().displayName()).isEqualTo("Updated Name");
  }

  @Test
  void save_preservesNullableFields_whenSetToNull() {
    adapter = new UserProfilePersistenceAdapter(jpaRepository, new UserProfileMapper());

    UserProfile profile =
        new UserProfile(
            UUID.randomUUID().toString(),
            "Minimal Profile",
            null,
            null,
            Instant.now(),
            Instant.now());

    adapter.save(profile);

    Optional<UserProfile> reloaded = adapter.findByUserId(profile.userId());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().bio()).isNull();
    assertThat(reloaded.get().avatarUrl()).isNull();
  }
}
