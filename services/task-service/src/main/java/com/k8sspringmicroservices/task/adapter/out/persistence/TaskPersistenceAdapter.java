package com.k8sspringmicroservices.task.adapter.out.persistence;

import com.k8sspringmicroservices.task.application.port.out.TaskRepositoryPort;
import com.k8sspringmicroservices.task.domain.Task;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TaskPersistenceAdapter implements TaskRepositoryPort {

  private final TaskJpaRepository jpaRepository;
  private final TaskMapper mapper;

  public TaskPersistenceAdapter(TaskJpaRepository jpaRepository, TaskMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Task save(Task task) {
    TaskEntity saved = jpaRepository.save(mapper.toEntity(task));
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<Task> findById(String id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Task> findAllByOwnerId(String ownerId) {
    return jpaRepository.findAllByOwnerId(ownerId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public boolean existsById(String id) {
    return jpaRepository.existsById(id);
  }

  @Override
  public void deleteById(String id) {
    jpaRepository.deleteById(id);
  }
}
