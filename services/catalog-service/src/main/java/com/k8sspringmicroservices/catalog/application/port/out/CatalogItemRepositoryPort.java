package com.k8sspringmicroservices.catalog.application.port.out;

import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import java.util.List;
import java.util.Optional;

public interface CatalogItemRepositoryPort {

  CatalogItem save(CatalogItem item);

  Optional<CatalogItem> findById(String id);

  List<CatalogItem> findAll();

  boolean existsById(String id);

  void deleteById(String id);
}
