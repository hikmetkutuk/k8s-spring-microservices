package com.k8sspringmicroservices.catalog.adapter.in.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.k8sspringmicroservices.catalog.adapter.in.web.dto.CreateCatalogItemRequest;
import com.k8sspringmicroservices.catalog.adapter.in.web.dto.UpdateCatalogItemRequest;
import com.k8sspringmicroservices.catalog.application.port.in.CatalogItemUseCase;
import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import com.k8sspringmicroservices.common.exception.ApplicationException;
import com.k8sspringmicroservices.common.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// @PreAuthorize enforcement gerektirir @EnableMethodSecurity + gercek SecurityFilterChain; bu
// slice'da yuklenmedigi icin rol kontrolu burada test edilmiyor, sadece request/response mapping
// ve validation davranisi test ediliyor.
@WebMvcTest(CatalogItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CatalogItemControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CatalogItemUseCase catalogItemUseCase;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static CatalogItem sampleItem() {
    return new CatalogItem(
        "item-1", "Widget", "desc", new BigDecimal("9.99"), 10, Instant.now(), Instant.now());
  }

  @Test
  void create_returns201() throws Exception {
    when(catalogItemUseCase.create(anyString(), anyString(), any(BigDecimal.class), anyInt()))
        .thenReturn(sampleItem());

    mockMvc
        .perform(
            post("/catalog-items")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new CreateCatalogItemRequest(
                            "Widget", "desc", new BigDecimal("9.99"), 10))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id", is("item-1")))
        .andExpect(jsonPath("$.data.name", is("Widget")));
  }

  @Test
  void create_returns400_whenNameMissing() throws Exception {
    mockMvc
        .perform(
            post("/catalog-items")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new CreateCatalogItemRequest("", "desc", new BigDecimal("9.99"), 10))))
        .andExpect(status().isBadRequest());

    verify(catalogItemUseCase, never()).create(any(), any(), any(), anyInt());
  }

  @Test
  void get_returns200() throws Exception {
    when(catalogItemUseCase.get("item-1")).thenReturn(sampleItem());

    mockMvc
        .perform(get("/catalog-items/item-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id", is("item-1")));
  }

  @Test
  void get_returns404_whenNotFound() throws Exception {
    when(catalogItemUseCase.get("missing"))
        .thenThrow(new ApplicationException(HttpStatus.NOT_FOUND, "Catalog item not found"));

    mockMvc.perform(get("/catalog-items/missing")).andExpect(status().isNotFound());
  }

  @Test
  void list_returns200_withItems() throws Exception {
    when(catalogItemUseCase.list()).thenReturn(List.of(sampleItem()));

    mockMvc
        .perform(get("/catalog-items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data", hasSize(1)))
        .andExpect(jsonPath("$.data[0].id", is("item-1")));
  }

  @Test
  void update_returns200() throws Exception {
    when(catalogItemUseCase.update(
            anyString(), anyString(), anyString(), any(BigDecimal.class), anyInt()))
        .thenReturn(sampleItem());

    mockMvc
        .perform(
            put("/catalog-items/item-1")
                .contentType("application/json")
                .content(
                    objectMapper.writeValueAsString(
                        new UpdateCatalogItemRequest(
                            "Widget2", "desc2", new BigDecimal("12.50"), 5))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id", is("item-1")));
  }

  @Test
  void delete_returns204() throws Exception {
    mockMvc.perform(delete("/catalog-items/item-1")).andExpect(status().isNoContent());

    verify(catalogItemUseCase).delete("item-1");
  }
}
