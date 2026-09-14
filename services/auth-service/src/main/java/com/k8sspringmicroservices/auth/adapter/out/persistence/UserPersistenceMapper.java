package com.k8sspringmicroservices.auth.adapter.out.persistence;

import com.k8sspringmicroservices.auth.domain.User;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

  public User toDomain(UserEntity entity) {
    return new User(
        entity.getId(),
        entity.getUsername(),
        entity.getEmail(),
        entity.getPasswordHash(),
        entity.getRoles(),
        entity.isEnabled());
  }

  public UserEntity toEntity(User user) {
    return new UserEntity(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getRoles(),
        user.isEnabled());
  }
}
