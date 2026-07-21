package com.k8sspringmicroservices.task.domain;

import java.math.BigDecimal;

public record CatalogItemSummary(String id, String name, BigDecimal price, int quantity) {}
