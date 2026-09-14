package com.k8sspringmicroservices.auth.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, String> {

  boolean existsByUsername(String username);

  Optional<UserEntity> findByUsername(String username);
}
