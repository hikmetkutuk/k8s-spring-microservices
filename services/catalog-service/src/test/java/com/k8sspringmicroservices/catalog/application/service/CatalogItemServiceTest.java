package com.k8sspringmicroservices.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.k8sspringmicroservices.catalog.application.port.out.CatalogItemRepositoryPort;
import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import com.k8sspringmicroservices.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogItemServiceTest {

  @Mock private CatalogItemRepositoryPort repository;

  private CatalogItemService service;

  @BeforeEach
  void setUp() {
    service = new CatalogItemService(repository);
  }

  @Test
  void create_savesNewItem_withGeneratedIdAndTimestamps() {
    when(repository.save(any(CatalogItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CatalogItem result = service.create("Widget", "desc", BigDecimal.TEN, 5);

    assertThat(result.id()).isNotBlank();
    assertThat(result.name()).isEqualTo("Widget");
    assertThat(result.createdAt()).isEqualTo(result.updatedAt());
  }

  @Test
  void get_returnsItem_whenFound() {
    CatalogItem item =
        new CatalogItem("c-1", "Widget", "desc", BigDecimal.TEN, 5, Instant.now(), Instant.now());
    when(repository.findById("c-1")).thenReturn(Optional.of(item));

    assertThat(service.get("c-1")).isEqualTo(item);
  }

  @Test
  void get_throwsResourceNotFound_whenMissing() {
    when(repository.findById("c-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get("c-1")).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void list_returnsAllItemsFromRepository() {
    CatalogItem item =
        new CatalogItem("c-1", "Widget", "desc", BigDecimal.TEN, 5, Instant.now(), Instant.now());
    when(repository.findAll()).thenReturn(List.of(item));

    assertThat(service.list()).containsExactly(item);
  }

  @Test
  void update_preservesCreatedAt_andAppliesNewValues() {
    Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
    CatalogItem existing =
        new CatalogItem("c-1", "Widget", "desc", BigDecimal.TEN, 5, createdAt, createdAt);
    when(repository.findById("c-1")).thenReturn(Optional.of(existing));
    when(repository.save(any(CatalogItem.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CatalogItem updated = service.update("c-1", "Widget2", "desc2", BigDecimal.ONE, 10);

    assertThat(updated.name()).isEqualTo("Widget2");
    assertThat(updated.price()).isEqualTo(BigDecimal.ONE);
    assertThat(updated.createdAt()).isEqualTo(createdAt);
    assertThat(updated.updatedAt()).isAfterOrEqualTo(createdAt);
  }

  @Test
  void update_throwsResourceNotFound_whenMissing() {
    when(repository.findById("c-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update("c-1", "Widget2", "desc2", BigDecimal.ONE, 10))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void delete_deletes_whenExists() {
    when(repository.existsById("c-1")).thenReturn(true);

    service.delete("c-1");

    verify(repository).deleteById("c-1");
  }

  @Test
  void delete_throwsResourceNotFound_whenMissing() {
    when(repository.existsById("c-1")).thenReturn(false);

    assertThatThrownBy(() -> service.delete("c-1")).isInstanceOf(ResourceNotFoundException.class);

    verify(repository, never()).deleteById(any());
  }
}
