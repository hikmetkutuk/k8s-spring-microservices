package com.k8sspringmicroservices.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.k8sspringmicroservices.common.exception.ConflictException;
import com.k8sspringmicroservices.common.exception.ResourceNotFoundException;
import com.k8sspringmicroservices.user.application.port.out.UserProfileRepositoryPort;
import com.k8sspringmicroservices.user.domain.UserProfile;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

  @Mock private UserProfileRepositoryPort repository;

  private UserProfileService service;

  @BeforeEach
  void setUp() {
    service = new UserProfileService(repository);
  }

  @Test
  void createProfile_savesNewProfile_whenNoneExists() {
    when(repository.existsByUserId("u-1")).thenReturn(false);
    when(repository.save(any(UserProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserProfile result = service.createProfile("u-1", "Alice", "bio", "avatar.png");

    assertThat(result.userId()).isEqualTo("u-1");
    assertThat(result.displayName()).isEqualTo("Alice");
    assertThat(result.createdAt()).isEqualTo(result.updatedAt());
  }

  @Test
  void createProfile_throwsConflict_whenProfileAlreadyExists() {
    when(repository.existsByUserId("u-1")).thenReturn(true);

    assertThatThrownBy(() -> service.createProfile("u-1", "Alice", "bio", "avatar.png"))
        .isInstanceOf(ConflictException.class);

    verify(repository, never()).save(any());
  }

  @Test
  void getProfile_returnsProfile_whenFound() {
    UserProfile profile =
        new UserProfile("u-1", "Alice", "bio", "avatar.png", Instant.now(), Instant.now());
    when(repository.findByUserId("u-1")).thenReturn(Optional.of(profile));

    assertThat(service.getProfile("u-1")).isEqualTo(profile);
  }

  @Test
  void getProfile_throwsResourceNotFound_whenMissing() {
    when(repository.findByUserId("u-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProfile("u-1"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void updateProfile_preservesCreatedAt_andUpdatesFields() {
    Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
    UserProfile existing =
        new UserProfile("u-1", "Alice", "bio", "avatar.png", createdAt, createdAt);
    when(repository.findByUserId("u-1")).thenReturn(Optional.of(existing));
    when(repository.save(any(UserProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserProfile updated = service.updateProfile("u-1", "Alice2", "new bio", "avatar2.png");

    assertThat(updated.displayName()).isEqualTo("Alice2");
    assertThat(updated.bio()).isEqualTo("new bio");
    assertThat(updated.createdAt()).isEqualTo(createdAt);
    assertThat(updated.updatedAt()).isAfterOrEqualTo(createdAt);
  }

  @Test
  void updateProfile_throwsResourceNotFound_whenProfileMissing() {
    when(repository.findByUserId("u-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateProfile("u-1", "Alice2", "bio", "avatar.png"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteProfile_deletes_whenExists() {
    when(repository.existsByUserId("u-1")).thenReturn(true);

    service.deleteProfile("u-1");

    verify(repository).deleteByUserId("u-1");
  }

  @Test
  void deleteProfile_throwsResourceNotFound_whenMissing() {
    when(repository.existsByUserId("u-1")).thenReturn(false);

    assertThatThrownBy(() -> service.deleteProfile("u-1"))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(repository, never()).deleteByUserId(any());
  }
}
