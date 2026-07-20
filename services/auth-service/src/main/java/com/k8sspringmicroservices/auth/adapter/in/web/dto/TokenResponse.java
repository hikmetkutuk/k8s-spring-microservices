package com.k8sspringmicroservices.auth.adapter.in.web.dto;

import com.k8sspringmicroservices.auth.application.port.in.AuthUseCase;

public record TokenResponse(
    String accessToken, String refreshToken, long expiresIn, String tokenType) {

  public static TokenResponse from(AuthUseCase.TokenPair tokenPair) {
    return new TokenResponse(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.accessTokenExpiresInSeconds(),
        "Bearer");
  }
}
