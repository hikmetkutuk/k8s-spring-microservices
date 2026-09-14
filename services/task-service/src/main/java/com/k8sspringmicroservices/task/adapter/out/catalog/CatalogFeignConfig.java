package com.k8sspringmicroservices.task.adapter.out.catalog;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class CatalogFeignConfig {

  @Bean
  public RequestInterceptor authorizationForwardingInterceptor() {
    return requestTemplate -> {
      if (RequestContextHolder.getRequestAttributes()
          instanceof ServletRequestAttributes attributes) {
        String authorizationHeader = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null) {
          requestTemplate.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
      }
    };
  }
}
