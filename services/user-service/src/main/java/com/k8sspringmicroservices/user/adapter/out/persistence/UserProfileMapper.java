package com.k8sspringmicroservices.user.adapter.out.persistence;

import com.k8sspringmicroservices.user.domain.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

  public UserProfile toDomain(UserProfileEntity entity) {
    return new UserProfile(
        entity.getUserId(),
        entity.getDisplayName(),
        entity.getBio(),
        entity.getAvatarUrl(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public UserProfileEntity toEntity(UserProfile profile) {
    return new UserProfileEntity(
        profile.userId(),
        profile.displayName(),
        profile.bio(),
        profile.avatarUrl(),
        profile.createdAt(),
        profile.updatedAt());
  }
}
