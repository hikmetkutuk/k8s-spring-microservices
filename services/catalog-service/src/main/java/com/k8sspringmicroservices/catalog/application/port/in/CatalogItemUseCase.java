package com.k8sspringmicroservices.catalog.application.port.in;

import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import java.math.BigDecimal;
import java.util.List;

public interface CatalogItemUseCase {

  CatalogItem create(String name, String description, BigDecimal price, int quantity);

  CatalogItem get(String id);

  List<CatalogItem> list();

  CatalogItem update(String id, String name, String description, BigDecimal price, int quantity);

  void delete(String id);
}
