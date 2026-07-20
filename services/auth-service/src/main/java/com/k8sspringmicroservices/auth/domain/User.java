package com.k8sspringmicroservices.auth.domain;

import java.util.Set;

public class User {

  private final String id;
  private final String username;
  private final String email;
  private final String passwordHash;
  private final Set<String> roles;
  private final boolean enabled;

  public User(
      String id,
      String username,
      String email,
      String passwordHash,
      Set<String> roles,
      boolean enabled) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.roles = roles;
    this.enabled = enabled;
  }

  public String getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
