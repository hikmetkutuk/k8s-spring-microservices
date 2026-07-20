package com.k8sspringmicroservices.user.application.service;

import com.k8sspringmicroservices.common.exception.ConflictException;
import com.k8sspringmicroservices.common.exception.ResourceNotFoundException;
import com.k8sspringmicroservices.user.application.port.in.UserProfileUseCase;
import com.k8sspringmicroservices.user.application.port.out.UserProfileRepositoryPort;
import com.k8sspringmicroservices.user.domain.UserProfile;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService implements UserProfileUseCase {

  private final UserProfileRepositoryPort repository;

  public UserProfileService(UserProfileRepositoryPort repository) {
    this.repository = repository;
  }

  @Override
  public UserProfile createProfile(
      String userId, String displayName, String bio, String avatarUrl) {
    if (repository.existsByUserId(userId)) {
      throw new ConflictException("Profile already exists for user: " + userId);
    }

    Instant now = Instant.now();
    UserProfile profile = new UserProfile(userId, displayName, bio, avatarUrl, now, now);
    return repository.save(profile);
  }

  @Override
  public UserProfile getProfile(String userId) {
    return repository
        .findByUserId(userId)
        .orElseThrow(() -> ResourceNotFoundException.forId("UserProfile", userId));
  }

  @Override
  public UserProfile updateProfile(
      String userId, String displayName, String bio, String avatarUrl) {
    UserProfile existing = getProfile(userId);
    UserProfile updated =
        new UserProfile(userId, displayName, bio, avatarUrl, existing.createdAt(), Instant.now());
    return repository.save(updated);
  }

  @Override
  public void deleteProfile(String userId) {
    if (!repository.existsByUserId(userId)) {
      throw ResourceNotFoundException.forId("UserProfile", userId);
    }
    repository.deleteByUserId(userId);
  }
}
