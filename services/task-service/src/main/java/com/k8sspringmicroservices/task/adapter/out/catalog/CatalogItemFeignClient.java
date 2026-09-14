package com.k8sspringmicroservices.task.adapter.out.catalog;

import com.k8sspringmicroservices.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "catalog-service",
    url = "${catalog-service.url}",
    configuration = CatalogFeignConfig.class)
public interface CatalogItemFeignClient {

  @GetMapping("/catalog-items/{id}")
  ApiResponse<CatalogItemFeignResponse> getItem(@PathVariable("id") String id);
}
