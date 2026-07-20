package com.k8sspringmicroservices.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtValidationProperties {

  /** auth-service'in RS256 imzasını doğrulamak için kullanılan public key konumu. */
  private String publicKeyLocation;

  public String getPublicKeyLocation() {
    return publicKeyLocation;
  }

  public void setPublicKeyLocation(String publicKeyLocation) {
    this.publicKeyLocation = publicKeyLocation;
  }
}
