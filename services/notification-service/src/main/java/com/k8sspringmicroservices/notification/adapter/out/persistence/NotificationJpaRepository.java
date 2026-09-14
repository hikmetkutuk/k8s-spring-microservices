package com.k8sspringmicroservices.notification.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {
  boolean existsByTaskId(String taskId);

  List<NotificationEntity> findAllByOwnerId(String ownerId);
}
