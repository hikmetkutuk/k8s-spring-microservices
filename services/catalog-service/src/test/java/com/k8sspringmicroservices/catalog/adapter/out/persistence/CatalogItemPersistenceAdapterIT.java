package com.k8sspringmicroservices.catalog.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CatalogItemPersistenceAdapterIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @DynamicPropertySource
  static void flywayProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired private CatalogItemJpaRepository jpaRepository;

  private CatalogItemPersistenceAdapter adapter;

  @Test
  void savesAndReloadsCatalogItem_roundTripsAllFields() {
    adapter = new CatalogItemPersistenceAdapter(jpaRepository, new CatalogItemMapper());

    CatalogItem item =
        new CatalogItem(
            UUID.randomUUID().toString(),
            "Test Widget",
            "A test widget for integration testing",
            new BigDecimal("19.99"),
            10,
            Instant.now(),
            Instant.now());

    CatalogItem saved = adapter.save(item);

    assertThat(saved).isNotNull();
    assertThat(saved.id()).isEqualTo(item.id());
    assertThat(saved.name()).isEqualTo("Test Widget");
    assertThat(saved.description()).isEqualTo("A test widget for integration testing");
    assertThat(saved.price()).isEqualByComparingTo(new BigDecimal("19.99"));
    assertThat(saved.quantity()).isEqualTo(10);

    Optional<CatalogItem> reloaded = adapter.findById(item.id());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().id()).isEqualTo(item.id());
    assertThat(reloaded.get().name()).isEqualTo(item.name());
    assertThat(reloaded.get().price()).isEqualByComparingTo(item.price());

    assertThat(adapter.existsById(item.id())).isTrue();
  }

  @Test
  void findAll_returnsAllSavedItems() {
    adapter = new CatalogItemPersistenceAdapter(jpaRepository, new CatalogItemMapper());

    CatalogItem item1 =
        new CatalogItem(
            UUID.randomUUID().toString(),
            "Item One",
            null,
            BigDecimal.ONE,
            1,
            Instant.now(),
            Instant.now());
    CatalogItem item2 =
        new CatalogItem(
            UUID.randomUUID().toString(),
            "Item Two",
            null,
            BigDecimal.TEN,
            5,
            Instant.now(),
            Instant.now());

    adapter.save(item1);
    adapter.save(item2);

    List<CatalogItem> all = adapter.findAll();
    assertThat(all).hasSize(2);
    assertThat(all).extracting(CatalogItem::id).containsExactlyInAnyOrder(item1.id(), item2.id());
  }

  @Test
  void findById_returnsEmpty_whenItemDoesNotExist() {
    adapter = new CatalogItemPersistenceAdapter(jpaRepository, new CatalogItemMapper());

    assertThat(adapter.findById(UUID.randomUUID().toString())).isEmpty();
  }

  @Test
  void existsById_returnsFalse_whenItemDoesNotExist() {
    adapter = new CatalogItemPersistenceAdapter(jpaRepository, new CatalogItemMapper());

    assertThat(adapter.existsById(UUID.randomUUID().toString())).isFalse();
  }

  @Test
  void deleteById_removesItem() {
    adapter = new CatalogItemPersistenceAdapter(jpaRepository, new CatalogItemMapper());

    CatalogItem item =
        new CatalogItem(
            UUID.randomUUID().toString(),
            "To Delete",
            null,
            BigDecimal.ZERO,
            0,
            Instant.now(),
            Instant.now());

    adapter.save(item);
    assertThat(adapter.existsById(item.id())).isTrue();

    adapter.deleteById(item.id());

    assertThat(adapter.existsById(item.id())).isFalse();
    assertThat(adapter.findById(item.id())).isEmpty();
  }

  @Test
  void save_updatesExistingItem_whenSameIdSavedAgain() {
    adapter = new CatalogItemPersistenceAdapter(jpaRepository, new CatalogItemMapper());

    CatalogItem original =
        new CatalogItem(
            UUID.randomUUID().toString(),
            "Original",
            "desc",
            new BigDecimal("10.00"),
            3,
            Instant.now(),
            Instant.now());

    adapter.save(original);

    CatalogItem updated =
        new CatalogItem(
            original.id(),
            "Updated Name",
            "updated desc",
            new BigDecimal("25.00"),
            42,
            original.createdAt(),
            Instant.now());

    CatalogItem result = adapter.save(updated);

    assertThat(result.name()).isEqualTo("Updated Name");
    assertThat(result.price()).isEqualByComparingTo(new BigDecimal("25.00"));
    assertThat(result.quantity()).isEqualTo(42);

    Optional<CatalogItem> reloaded = adapter.findById(original.id());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().name()).isEqualTo("Updated Name");
  }
}
