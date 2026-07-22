package com.k8sspringmicroservices.notification.adapter.in.web.dto;

import com.k8sspringmicroservices.notification.domain.Notification;
import java.time.Instant;

public record NotificationResponse(
    String id, String taskId, String ownerId, String message, Instant createdAt) {

  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.id(),
        notification.taskId(),
        notification.ownerId(),
        notification.message(),
        notification.createdAt());
  }
}
