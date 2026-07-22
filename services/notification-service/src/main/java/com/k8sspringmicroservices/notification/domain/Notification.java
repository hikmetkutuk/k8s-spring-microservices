package com.k8sspringmicroservices.notification.domain;

import java.time.Instant;

public record Notification(
    String id, String taskId, String ownerId, String message, Instant createdAt) {}
