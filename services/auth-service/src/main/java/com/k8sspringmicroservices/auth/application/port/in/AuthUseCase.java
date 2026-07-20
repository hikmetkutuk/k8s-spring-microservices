package com.k8sspringmicroservices.auth.application.port.in;

public interface AuthUseCase {

  TokenPair register(String username, String email, String rawPassword);

  TokenPair login(String username, String rawPassword);

  TokenPair refresh(String refreshToken);

  void logout(String refreshToken);

  record TokenPair(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {}
}
