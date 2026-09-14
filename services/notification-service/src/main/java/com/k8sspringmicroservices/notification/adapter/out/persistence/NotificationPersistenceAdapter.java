package com.k8sspringmicroservices.notification.adapter.out.persistence;

import com.k8sspringmicroservices.notification.application.port.out.NotificationRepositoryPort;
import com.k8sspringmicroservices.notification.domain.Notification;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationPersistenceAdapter implements NotificationRepositoryPort {

  private final NotificationJpaRepository jpaRepository;
  private final NotificationMapper mapper;

  public NotificationPersistenceAdapter(
      NotificationJpaRepository jpaRepository, NotificationMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Notification save(Notification notification) {
    NotificationEntity saved = jpaRepository.save(mapper.toEntity(notification));
    return mapper.toDomain(saved);
  }

  @Override
  public boolean existsByTaskId(String taskId) {
    return jpaRepository.existsByTaskId(taskId);
  }

  @Override
  public List<Notification> findAllByOwnerId(String ownerId) {
    return jpaRepository.findAllByOwnerId(ownerId).stream().map(mapper::toDomain).toList();
  }
}
