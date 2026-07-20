package com.k8sspringmicroservices.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  private String privateKeyLocation;
  private String publicKeyLocation;
  private String issuer = "auth-service";
  private long accessTokenTtlSeconds = 900; // 15 dakika
  private long refreshTokenTtlSeconds = 604800; // 7 gün

  public String getPrivateKeyLocation() {
    return privateKeyLocation;
  }

  public void setPrivateKeyLocation(String privateKeyLocation) {
    this.privateKeyLocation = privateKeyLocation;
  }

  public String getPublicKeyLocation() {
    return publicKeyLocation;
  }

  public void setPublicKeyLocation(String publicKeyLocation) {
    this.publicKeyLocation = publicKeyLocation;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public long getAccessTokenTtlSeconds() {
    return accessTokenTtlSeconds;
  }

  public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
    this.accessTokenTtlSeconds = accessTokenTtlSeconds;
  }

  public long getRefreshTokenTtlSeconds() {
    return refreshTokenTtlSeconds;
  }

  public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
    this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
  }
}
