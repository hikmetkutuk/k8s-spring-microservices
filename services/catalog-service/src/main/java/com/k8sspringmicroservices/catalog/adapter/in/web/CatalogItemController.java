package com.k8sspringmicroservices.catalog.adapter.in.web;

import com.k8sspringmicroservices.catalog.adapter.in.web.dto.CatalogItemResponse;
import com.k8sspringmicroservices.catalog.adapter.in.web.dto.CreateCatalogItemRequest;
import com.k8sspringmicroservices.catalog.adapter.in.web.dto.UpdateCatalogItemRequest;
import com.k8sspringmicroservices.catalog.application.port.in.CatalogItemUseCase;
import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import com.k8sspringmicroservices.common.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog-items")
public class CatalogItemController {

  private final CatalogItemUseCase catalogItemUseCase;

  public CatalogItemController(CatalogItemUseCase catalogItemUseCase) {
    this.catalogItemUseCase = catalogItemUseCase;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<CatalogItemResponse>> create(
      @Valid @RequestBody CreateCatalogItemRequest request) {
    CatalogItem item =
        catalogItemUseCase.create(
            request.name(), request.description(), request.price(), request.quantity());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(CatalogItemResponse.from(item)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CatalogItemResponse>> get(@PathVariable String id) {
    CatalogItem item = catalogItemUseCase.get(id);
    return ResponseEntity.ok(ApiResponse.of(CatalogItemResponse.from(item)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> list() {
    List<CatalogItemResponse> items =
        catalogItemUseCase.list().stream().map(CatalogItemResponse::from).toList();
    return ResponseEntity.ok(ApiResponse.of(items));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<CatalogItemResponse>> update(
      @PathVariable String id, @Valid @RequestBody UpdateCatalogItemRequest request) {
    CatalogItem item =
        catalogItemUseCase.update(
            id, request.name(), request.description(), request.price(), request.quantity());
    return ResponseEntity.ok(ApiResponse.of(CatalogItemResponse.from(item)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    catalogItemUseCase.delete(id);
    return ResponseEntity.noContent().build();
  }
}
