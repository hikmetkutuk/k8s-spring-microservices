package com.k8sspringmicroservices.task.adapter.out.catalog;

import java.math.BigDecimal;

public record CatalogItemFeignResponse(String id, String name, BigDecimal price, int quantity) {}
