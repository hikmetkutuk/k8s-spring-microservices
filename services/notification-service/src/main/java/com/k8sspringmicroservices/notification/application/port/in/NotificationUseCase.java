package com.k8sspringmicroservices.notification.application.port.in;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.notification.domain.Notification;
import java.util.List;

public interface NotificationUseCase {
  void handleTaskCreated(TaskCreatedEvent event);

  List<Notification> listByOwner(String ownerId);
}
