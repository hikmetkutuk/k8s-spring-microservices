package com.k8sspringmicroservices.user.domain;

import java.time.Instant;

public record UserProfile(
    String userId,
    String displayName,
    String bio,
    String avatarUrl,
    Instant createdAt,
    Instant updatedAt) {}
