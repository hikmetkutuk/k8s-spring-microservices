package com.k8sspringmicroservices.task.domain;

import java.time.Instant;

public record Task(
    String id,
    String ownerId,
    String catalogItemId,
    String title,
    String description,
    int quantity,
    TaskStatus status,
    Instant createdAt,
    Instant updatedAt) {}
