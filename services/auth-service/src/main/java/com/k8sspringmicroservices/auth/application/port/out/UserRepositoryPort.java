package com.k8sspringmicroservices.auth.application.port.out;

import com.k8sspringmicroservices.auth.domain.User;
import java.util.Optional;

public interface UserRepositoryPort {

  boolean existsByUsername(String username);

  User save(User user);

  Optional<User> findByUsername(String username);

  Optional<User> findById(String id);
}
