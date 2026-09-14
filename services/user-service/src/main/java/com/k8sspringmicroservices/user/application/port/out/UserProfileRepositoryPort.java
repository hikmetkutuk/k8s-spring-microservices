package com.k8sspringmicroservices.user.application.port.out;

import com.k8sspringmicroservices.user.domain.UserProfile;
import java.util.Optional;

public interface UserProfileRepositoryPort {

  boolean existsByUserId(String userId);

  UserProfile save(UserProfile profile);

  Optional<UserProfile> findByUserId(String userId);

  void deleteByUserId(String userId);
}
