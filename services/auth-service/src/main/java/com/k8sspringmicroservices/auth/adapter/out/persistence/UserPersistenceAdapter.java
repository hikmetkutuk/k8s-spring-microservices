package com.k8sspringmicroservices.auth.adapter.out.persistence;

import com.k8sspringmicroservices.auth.application.port.out.UserRepositoryPort;
import com.k8sspringmicroservices.auth.domain.User;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

  private final UserJpaRepository jpaRepository;
  private final UserPersistenceMapper mapper;

  public UserPersistenceAdapter(UserJpaRepository jpaRepository, UserPersistenceMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public boolean existsByUsername(String username) {
    return jpaRepository.existsByUsername(username);
  }

  @Override
  public User save(User user) {
    UserEntity saved = jpaRepository.save(mapper.toEntity(user));
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return jpaRepository.findByUsername(username).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findById(String id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }
}
