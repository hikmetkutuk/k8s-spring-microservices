package com.k8sspringmicroservices.notification.adapter.out.persistence;

import com.k8sspringmicroservices.notification.domain.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

  public Notification toDomain(NotificationEntity entity) {
    return new Notification(
        entity.getId(),
        entity.getTaskId(),
        entity.getOwnerId(),
        entity.getMessage(),
        entity.getCreatedAt());
  }

  public NotificationEntity toEntity(Notification notification) {
    return new NotificationEntity(
        notification.id(),
        notification.taskId(),
        notification.ownerId(),
        notification.message(),
        notification.createdAt());
  }
}
