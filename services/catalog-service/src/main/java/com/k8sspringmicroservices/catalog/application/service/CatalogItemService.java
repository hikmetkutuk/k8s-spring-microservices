package com.k8sspringmicroservices.catalog.application.service;

import com.k8sspringmicroservices.catalog.application.port.in.CatalogItemUseCase;
import com.k8sspringmicroservices.catalog.application.port.out.CatalogItemRepositoryPort;
import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import com.k8sspringmicroservices.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CatalogItemService implements CatalogItemUseCase {

  private static final String CACHE_NAME = "catalogItems";

  private final CatalogItemRepositoryPort repository;

  public CatalogItemService(CatalogItemRepositoryPort repository) {
    this.repository = repository;
  }

  @Override
  @CachePut(value = CACHE_NAME, key = "#result.id()")
  public CatalogItem create(String name, String description, BigDecimal price, int quantity) {
    Instant now = Instant.now();
    CatalogItem item =
        new CatalogItem(UUID.randomUUID().toString(), name, description, price, quantity, now, now);
    return repository.save(item);
  }

  @Override
  @Cacheable(value = CACHE_NAME, key = "#id")
  public CatalogItem get(String id) {
    return findExistingOrThrow(id);
  }

  @Override
  public List<CatalogItem> list() {
    return repository.findAll();
  }

  @Override
  @CachePut(value = CACHE_NAME, key = "#id")
  public CatalogItem update(
      String id, String name, String description, BigDecimal price, int quantity) {
    CatalogItem existing = findExistingOrThrow(id);
    CatalogItem updated =
        new CatalogItem(
            id, name, description, price, quantity, existing.createdAt(), Instant.now());
    return repository.save(updated);
  }

  @Override
  @CacheEvict(value = CACHE_NAME, key = "#id")
  public void delete(String id) {
    if (!repository.existsById(id)) {
      throw ResourceNotFoundException.forId("CatalogItem", id);
    }
    repository.deleteById(id);
  }

  private CatalogItem findExistingOrThrow(String id) {
    return repository
        .findById(id)
        .orElseThrow(() -> ResourceNotFoundException.forId("CatalogItem", id));
  }
}
