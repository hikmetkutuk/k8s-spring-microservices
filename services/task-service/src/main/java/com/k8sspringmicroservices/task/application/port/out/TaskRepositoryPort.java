package com.k8sspringmicroservices.task.application.port.out;

import com.k8sspringmicroservices.task.domain.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepositoryPort {
  Task save(Task task);

  Optional<Task> findById(String id);

  List<Task> findAllByOwnerId(String ownerId);

  boolean existsById(String id);

  void deleteById(String id);
}
