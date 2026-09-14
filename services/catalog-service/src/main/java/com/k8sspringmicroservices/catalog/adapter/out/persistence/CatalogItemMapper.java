package com.k8sspringmicroservices.catalog.adapter.out.persistence;

import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import org.springframework.stereotype.Component;

@Component
public class CatalogItemMapper {

  public CatalogItem toDomain(CatalogItemEntity entity) {
    return new CatalogItem(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.getPrice(),
        entity.getQuantity(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public CatalogItemEntity toEntity(CatalogItem item) {
    return new CatalogItemEntity(
        item.id(),
        item.name(),
        item.description(),
        item.price(),
        item.quantity(),
        item.createdAt(),
        item.updatedAt());
  }
}
