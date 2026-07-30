package com.k8sspringmicroservices.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.k8sspringmicroservices.gateway.config.GatewaySecurityProperties;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JwtAuthenticationGatewayFilterTest {

  private KeyPair keyPair;
  private GatewaySecurityProperties properties;
  private JwtAuthenticationGatewayFilter filter;
  private GatewayFilterChain chain;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    keyPair = generator.generateKeyPair();

    properties = new GatewaySecurityProperties();
    properties.setPublicPaths(List.of("/auth"));

    filter = new JwtAuthenticationGatewayFilter(keyPair.getPublic(), properties);

    chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
  }

  @Test
  void filter_allowsRequest_toPublicPath_withoutToken() {
    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/auth/login").build());

    filter.filter(exchange, chain).block();

    verify(chain).filter(exchange);
  }

  @Test
  void filter_rejectsRequest_whenAuthorizationHeaderMissing() {
    ServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/tasks/me").build());

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(chain, never()).filter(any());
  }

  @Test
  void filter_rejectsRequest_whenTokenSignedByDifferentKey() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair otherKeyPair = generator.generateKeyPair();

    String token =
        Jwts.builder()
            .subject("user-1")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(otherKeyPair.getPrivate())
            .compact();

    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tasks/me")
                .header("Authorization", "Bearer " + token)
                .build());

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(chain, never()).filter(any());
  }

  @Test
  void filter_allowsRequest_whenTokenIsValid() {
    String token =
        Jwts.builder()
            .subject("user-1")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(keyPair.getPrivate())
            .compact();

    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tasks/me")
                .header("Authorization", "Bearer " + token)
                .build());

    filter.filter(exchange, chain).block();

    verify(chain, times(1)).filter(exchange);
  }

  @Test
  void filter_rejectsRequest_whenTokenExpired() {
    String token =
        Jwts.builder()
            .subject("user-1")
            .issuedAt(Date.from(Instant.now().minusSeconds(120)))
            .expiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(keyPair.getPrivate())
            .compact();

    ServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/tasks/me")
                .header("Authorization", "Bearer " + token)
                .build());

    filter.filter(exchange, chain).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
