package com.k8sspringmicroservices.notification.application.service;

import com.k8sspringmicroservices.common.event.TaskCreatedEvent;
import com.k8sspringmicroservices.notification.application.port.in.NotificationUseCase;
import com.k8sspringmicroservices.notification.application.port.out.NotificationRepositoryPort;
import com.k8sspringmicroservices.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class NotificationService implements NotificationUseCase {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepositoryPort repository;

  public NotificationService(NotificationRepositoryPort repository) {
    this.repository = repository;
  }

  @Override
  public void handleTaskCreated(TaskCreatedEvent event) {
    if (repository.existsByTaskId(event.taskId())) {
      log.info("Task {} için bildirim zaten işlenmiş, atlanıyor", event.taskId());
      return;
    }

    Notification notification =
        new Notification(
            UUID.randomUUID().toString(),
            event.taskId(),
            event.ownerId(),
            "Göreviniz oluşturuldu: " + event.title(),
            Instant.now());

    try {
      repository.save(notification);
      log.info(
          "Bildirim simülasyonu: kullanıcı {} için '{}' görevi bildirimi gönderildi",
          event.ownerId(),
          event.title());
    } catch (DataIntegrityViolationException e) {
      log.info("Task {} için bildirim eşzamanlı olarak zaten işlenmiş", event.taskId());
    }
  }

  @Override
  public List<Notification> listByOwner(String ownerId) {
    return repository.findAllByOwnerId(ownerId);
  }
}
