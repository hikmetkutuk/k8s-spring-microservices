package com.k8sspringmicroservices.auth.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private String id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
  @CollectionTable(
      name = "user_roles",
      joinColumns = @jakarta.persistence.JoinColumn(name = "user_id"))
  @Column(name = "role")
  private Set<String> roles;

  @Column(nullable = false)
  private boolean enabled;

  protected UserEntity() {}

  public UserEntity(
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
