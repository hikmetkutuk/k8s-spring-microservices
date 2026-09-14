package com.k8sspringmicroservices.catalog.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemJpaRepository extends JpaRepository<CatalogItemEntity, String> {}
