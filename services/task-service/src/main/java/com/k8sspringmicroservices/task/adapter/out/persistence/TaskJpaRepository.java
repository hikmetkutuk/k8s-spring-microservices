package com.k8sspringmicroservices.task.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, String> {
  List<TaskEntity> findAllByOwnerId(String ownerId);
}
