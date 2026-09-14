package com.k8sspringmicroservices.user.adapter.out.persistence;

import com.k8sspringmicroservices.user.application.port.out.UserProfileRepositoryPort;
import com.k8sspringmicroservices.user.domain.UserProfile;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserProfilePersistenceAdapter implements UserProfileRepositoryPort {

  private final UserProfileJpaRepository jpaRepository;
  private final UserProfileMapper mapper;

  public UserProfilePersistenceAdapter(
      UserProfileJpaRepository jpaRepository, UserProfileMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public boolean existsByUserId(String userId) {
    return jpaRepository.existsById(userId);
  }

  @Override
  public UserProfile save(UserProfile profile) {
    UserProfileEntity saved = jpaRepository.save(mapper.toEntity(profile));
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<UserProfile> findByUserId(String userId) {
    return jpaRepository.findById(userId).map(mapper::toDomain);
  }

  @Override
  public void deleteByUserId(String userId) {
    jpaRepository.deleteById(userId);
  }
}
