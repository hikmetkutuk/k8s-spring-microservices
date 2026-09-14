package com.k8sspringmicroservices.notification.application.port.out;

import com.k8sspringmicroservices.notification.domain.Notification;
import java.util.List;

public interface NotificationRepositoryPort {
  Notification save(Notification notification);

  boolean existsByTaskId(String taskId);

  List<Notification> findAllByOwnerId(String ownerId);
}
