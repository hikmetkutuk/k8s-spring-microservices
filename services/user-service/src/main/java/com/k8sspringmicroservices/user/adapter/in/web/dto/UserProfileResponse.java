package com.k8sspringmicroservices.user.adapter.in.web.dto;

import com.k8sspringmicroservices.user.domain.UserProfile;
import java.time.Instant;

public record UserProfileResponse(
    String userId,
    String displayName,
    String bio,
    String avatarUrl,
    Instant createdAt,
    Instant updatedAt) {

  public static UserProfileResponse from(UserProfile profile) {
    return new UserProfileResponse(
        profile.userId(),
        profile.displayName(),
        profile.bio(),
        profile.avatarUrl(),
        profile.createdAt(),
        profile.updatedAt());
  }
}
