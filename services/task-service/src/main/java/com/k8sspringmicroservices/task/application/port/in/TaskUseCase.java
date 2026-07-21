package com.k8sspringmicroservices.task.application.port.in;

import com.k8sspringmicroservices.task.domain.Task;
import java.util.List;

public interface TaskUseCase {
  Task create(String ownerId, String catalogItemId, String title, String description, int quantity);

  Task get(String id);

  List<Task> listByOwner(String ownerId);

  Task update(String id, String title, String description, int quantity);

  void delete(String id);
}
