package com.k8sspringmicroservices.gateway.filter;

import com.k8sspringmicroservices.common.security.SecurityConstants;
import com.k8sspringmicroservices.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.PublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationGatewayFilter.class);

  private final PublicKey publicKey;
  private final GatewaySecurityProperties properties;

  public JwtAuthenticationGatewayFilter(PublicKey publicKey, GatewaySecurityProperties properties) {
    this.publicKey = publicKey;
    this.properties = properties;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (isPublic(path)) {
      return chain.filter(exchange);
    }

    String header =
        exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
    if (header == null || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
      return unauthorized(exchange);
    }

    String token = header.substring(SecurityConstants.BEARER_PREFIX.length());
    try {
      Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("Gateway'de geçersiz JWT: {}", e.getMessage());
      return unauthorized(exchange);
    }

    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return -1;
  }

  private boolean isPublic(String path) {
    return properties.getPublicPaths().stream().anyMatch(path::startsWith);
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }
}
