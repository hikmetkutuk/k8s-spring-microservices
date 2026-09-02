package com.k8sspringmicroservices.auth.application.service;

import com.k8sspringmicroservices.auth.application.port.in.AuthUseCase;
import com.k8sspringmicroservices.auth.application.port.out.RefreshTokenStorePort;
import com.k8sspringmicroservices.auth.application.port.out.TokenProviderPort;
import com.k8sspringmicroservices.auth.application.port.out.UserRepositoryPort;
import com.k8sspringmicroservices.auth.domain.User;
import com.k8sspringmicroservices.common.exception.ApplicationException;
import com.k8sspringmicroservices.common.exception.ConflictException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements AuthUseCase {

  private final UserRepositoryPort userRepository;
  private final TokenProviderPort tokenProvider;
  private final RefreshTokenStorePort refreshTokenStore;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      UserRepositoryPort userRepository,
      TokenProviderPort tokenProvider,
      RefreshTokenStorePort refreshTokenStore,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.tokenProvider = tokenProvider;
    this.refreshTokenStore = refreshTokenStore;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public TokenPair register(String username, String email, String rawPassword) {
    if (userRepository.existsByUsername(username)) {
      throw new ConflictException("Username already taken: " + username);
    }

    User user =
        new User(
            UUID.randomUUID().toString(),
            username,
            email,
            passwordEncoder.encode(rawPassword),
            Set.of("USER"),
            true);

    User saved = userRepository.save(user);
    return issueTokenPair(saved);
  }

  @Override
  public TokenPair login(String username, String rawPassword) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> unauthorized("Invalid username or password"));

    if (!user.isEnabled() || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      throw unauthorized("Invalid username or password");
    }

    return issueTokenPair(user);
  }

  @Override
  public TokenPair refresh(String refreshToken) {
    String userId =
        refreshTokenStore
            .consume(refreshToken)
            .orElseThrow(() -> unauthorized("Invalid or expired refresh token"));

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> unauthorized("Invalid or expired refresh token"));

    if (!user.isEnabled()) {
      throw unauthorized("Account is disabled");
    }

    return issueTokenPair(user);
  }

  @Override
  public void logout(String refreshToken) {
    refreshTokenStore.revoke(refreshToken);
  }

  private TokenPair issueTokenPair(User user) {
    String accessToken = tokenProvider.generateAccessToken(user);
    String refreshToken = refreshTokenStore.issue(user.getId());
    return new TokenPair(
        accessToken, refreshToken, tokenProvider.getAccessTokenExpirationSeconds());
  }

  private ApplicationException unauthorized(String message) {
    return new ApplicationException(HttpStatus.UNAUTHORIZED, message);
  }
}
