package com.k8sspringmicroservices.user.application.port.in;

import com.k8sspringmicroservices.user.domain.UserProfile;

public interface UserProfileUseCase {

  UserProfile createProfile(String userId, String displayName, String bio, String avatarUrl);

  UserProfile getProfile(String userId);

  UserProfile updateProfile(String userId, String displayName, String bio, String avatarUrl);

  void deleteProfile(String userId);
}
