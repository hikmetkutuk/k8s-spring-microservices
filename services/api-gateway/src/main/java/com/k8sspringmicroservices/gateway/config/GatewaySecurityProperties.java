package com.k8sspringmicroservices.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

  /** JWT doğrulaması gerektirmeyen, herkese açık path prefix'leri. */
  private List<String> publicPaths = List.of();

  public List<String> getPublicPaths() {
    return publicPaths;
  }

  public void setPublicPaths(List<String> publicPaths) {
    this.publicPaths = publicPaths;
  }
}
