package com.k8sspringmicroservices.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenApiConfig {

  private static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI userServiceOpenApi() {
    return new OpenAPI()
        .info(new Info().title("User Service API").version("v1"))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }

  /**
   * Gateway arkasında proxy'lenirken OpenAPI "servers" adresinin istemciden gelen (sahte
   * olabilecek) Forwarded/X-Forwarded-* header'larına değil, operatör tarafından konfigüre edilen
   * sabit bir değere dayanmasını sağlar (bkz. helm values: config.SPRINGDOC_SERVER_BASE_URL). Değer
   * boşsa springdoc'un varsayılan otomatik tespiti kullanılır (plain local çalıştırmada).
   */
  @Bean
  public ServerBaseUrlCustomizer serverBaseUrlCustomizer(
      @Value("${springdoc.server-base-url:}") String configuredBaseUrl) {
    return (serverBaseUrl, request) ->
        StringUtils.hasText(configuredBaseUrl) ? configuredBaseUrl : serverBaseUrl;
  }
}
