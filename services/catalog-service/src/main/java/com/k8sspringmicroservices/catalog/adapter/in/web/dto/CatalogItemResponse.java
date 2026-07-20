package com.k8sspringmicroservices.catalog.adapter.in.web.dto;

import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import java.math.BigDecimal;
import java.time.Instant;

public record CatalogItemResponse(
    String id,
    String name,
    String description,
    BigDecimal price,
    int quantity,
    Instant createdAt,
    Instant updatedAt) {

  public static CatalogItemResponse from(CatalogItem item) {
    return new CatalogItemResponse(
        item.id(),
        item.name(),
        item.description(),
        item.price(),
        item.quantity(),
        item.createdAt(),
        item.updatedAt());
  }
}
