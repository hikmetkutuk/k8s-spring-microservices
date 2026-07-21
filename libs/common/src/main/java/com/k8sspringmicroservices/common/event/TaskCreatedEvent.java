package com.k8sspringmicroservices.common.event;

import java.time.Instant;

public record TaskCreatedEvent(
    String taskId,
    String ownerId,
    String catalogItemId,
    String title,
    int quantity,
    Instant createdAt) {}
