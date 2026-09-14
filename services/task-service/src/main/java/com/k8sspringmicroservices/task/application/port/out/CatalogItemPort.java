package com.k8sspringmicroservices.task.application.port.out;

import com.k8sspringmicroservices.task.domain.CatalogItemSummary;

public interface CatalogItemPort {
  CatalogItemSummary getItem(String catalogItemId);
}
