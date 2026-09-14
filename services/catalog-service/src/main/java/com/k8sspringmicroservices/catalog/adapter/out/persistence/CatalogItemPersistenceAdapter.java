package com.k8sspringmicroservices.catalog.adapter.out.persistence;

import com.k8sspringmicroservices.catalog.application.port.out.CatalogItemRepositoryPort;
import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CatalogItemPersistenceAdapter implements CatalogItemRepositoryPort {

  private final CatalogItemJpaRepository jpaRepository;
  private final CatalogItemMapper mapper;

  public CatalogItemPersistenceAdapter(
      CatalogItemJpaRepository jpaRepository, CatalogItemMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public CatalogItem save(CatalogItem item) {
    CatalogItemEntity saved = jpaRepository.save(mapper.toEntity(item));
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<CatalogItem> findById(String id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<CatalogItem> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
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
