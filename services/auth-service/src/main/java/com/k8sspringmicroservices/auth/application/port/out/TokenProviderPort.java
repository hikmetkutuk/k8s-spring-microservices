package com.k8sspringmicroservices.auth.application.port.out;

import com.k8sspringmicroservices.auth.domain.User;
import java.util.Optional;

public interface TokenProviderPort {

  String generateAccessToken(User user);

  long getAccessTokenExpirationSeconds();

  Optional<String> extractUserId(String token);
}
