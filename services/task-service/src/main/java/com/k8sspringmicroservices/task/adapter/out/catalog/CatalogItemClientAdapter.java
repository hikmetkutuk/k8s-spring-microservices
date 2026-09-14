package com.k8sspringmicroservices.task.adapter.out.catalog;

import com.k8sspringmicroservices.common.dto.ApiResponse;
import com.k8sspringmicroservices.common.exception.ResourceNotFoundException;
import com.k8sspringmicroservices.task.application.port.out.CatalogItemPort;
import com.k8sspringmicroservices.task.domain.CatalogItemSummary;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CatalogItemClientAdapter implements CatalogItemPort {

  private static final String CATALOG_SERVICE = "catalogService";

  private final CatalogItemFeignClient feignClient;

  public CatalogItemClientAdapter(CatalogItemFeignClient feignClient) {
    this.feignClient = feignClient;
  }

  @Override
  @CircuitBreaker(name = CATALOG_SERVICE, fallbackMethod = "getItemFallback")
  @Retry(name = CATALOG_SERVICE)
  public CatalogItemSummary getItem(String catalogItemId) {
    ApiResponse<CatalogItemFeignResponse> response = feignClient.getItem(catalogItemId);
    CatalogItemFeignResponse item = response.data();
    return new CatalogItemSummary(item.id(), item.name(), item.price(), item.quantity());
  }

  @SuppressWarnings("unused")
  private CatalogItemSummary getItemFallback(String catalogItemId, Throwable throwable) {
    if (throwable instanceof FeignException.NotFound) {
      throw ResourceNotFoundException.forId("CatalogItem", catalogItemId);
    }
    throw new ResponseStatusException(
        HttpStatus.SERVICE_UNAVAILABLE,
        "catalog-service şu anda kullanılamıyor, lütfen daha sonra tekrar deneyin",
        throwable);
  }
}
