package com.k8sspringmicroservices.user.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

  @Id
  @Column(name = "user_id", nullable = false, updatable = false)
  private String userId;

  @Column(name = "display_name")
  private String displayName;

  @Column(name = "bio")
  private String bio;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected UserProfileEntity() {}

  public UserProfileEntity(
      String userId,
      String displayName,
      String bio,
      String avatarUrl,
      Instant createdAt,
      Instant updatedAt) {
    this.userId = userId;
    this.displayName = displayName;
    this.bio = bio;
    this.avatarUrl = avatarUrl;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getUserId() {
    return userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getBio() {
    return bio;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
