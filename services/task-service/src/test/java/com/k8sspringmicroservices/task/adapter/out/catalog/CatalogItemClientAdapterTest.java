package com.k8sspringmicroservices.task.adapter.out.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.k8sspringmicroservices.common.dto.ApiResponse;
import com.k8sspringmicroservices.common.exception.ResourceNotFoundException;
import com.k8sspringmicroservices.task.domain.CatalogItemSummary;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CatalogItemClientAdapterTest {

  @Mock private CatalogItemFeignClient feignClient;

  @Test
  void getItem_mapsFeignResponseToDomainSummary() {
    CatalogItemClientAdapter realAdapter = new CatalogItemClientAdapter(feignClient);
    CatalogItemFeignResponse feignResponse =
        new CatalogItemFeignResponse("c-1", "Widget", BigDecimal.TEN, 5);
    when(feignClient.getItem("c-1")).thenReturn(ApiResponse.of(feignResponse));

    CatalogItemSummary result = realAdapter.getItem("c-1");

    assertThat(result).isEqualTo(new CatalogItemSummary("c-1", "Widget", BigDecimal.TEN, 5));
  }

  @Test
  void fallback_throwsResourceNotFound_whenUnderlyingCauseIsFeignNotFound() throws Exception {
    CatalogItemClientAdapter realAdapter = new CatalogItemClientAdapter(feignClient);
    FeignException.NotFound notFound =
        new FeignException.NotFound(
            "not found",
            Request.create(
                Request.HttpMethod.GET,
                "/catalog-items/c-1",
                Collections.emptyMap(),
                null,
                new RequestTemplate()),
            null,
            null);

    assertThatThrownBy(() -> invokeFallback(realAdapter, "c-1", notFound))
        .cause()
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void fallback_throwsServiceUnavailable_whenCauseIsGenericFailure() {
    CatalogItemClientAdapter realAdapter = new CatalogItemClientAdapter(feignClient);
    RuntimeException timeout = new RuntimeException("timeout");

    assertThatThrownBy(() -> invokeFallback(realAdapter, "c-1", timeout))
        .cause()
        .isInstanceOf(ResponseStatusException.class);
  }

  private void invokeFallback(
      CatalogItemClientAdapter adapter, String catalogItemId, Throwable cause) throws Exception {
    Method fallback =
        CatalogItemClientAdapter.class.getDeclaredMethod(
            "getItemFallback", String.class, Throwable.class);
    fallback.setAccessible(true);
    try {
      fallback.invoke(adapter, catalogItemId, cause);
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    }
  }
}
