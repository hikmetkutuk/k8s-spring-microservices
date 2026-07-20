package com.k8sspringmicroservices.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record CatalogItem(
    String id,
    String name,
    String description,
    BigDecimal price,
    int quantity,
    Instant createdAt,
    Instant updatedAt) {}
