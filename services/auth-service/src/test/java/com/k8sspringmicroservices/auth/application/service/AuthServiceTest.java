package com.k8sspringmicroservices.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.k8sspringmicroservices.auth.application.port.in.AuthUseCase.TokenPair;
import com.k8sspringmicroservices.auth.application.port.out.RefreshTokenStorePort;
import com.k8sspringmicroservices.auth.application.port.out.TokenProviderPort;
import com.k8sspringmicroservices.auth.application.port.out.UserRepositoryPort;
import com.k8sspringmicroservices.auth.domain.User;
import com.k8sspringmicroservices.common.exception.ApplicationException;
import com.k8sspringmicroservices.common.exception.ConflictException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private TokenProviderPort tokenProvider;
  @Mock private RefreshTokenStorePort refreshTokenStore;
  @Mock private PasswordEncoder passwordEncoder;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(userRepository, tokenProvider, refreshTokenStore, passwordEncoder);
  }

  @Test
  void register_savesNewUserAndIssuesTokenPair() {
    when(userRepository.existsByUsername("alice")).thenReturn(false);
    when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(tokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
    when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
    when(refreshTokenStore.issue(anyString())).thenReturn("refresh-token");

    TokenPair result = authService.register("alice", "alice@example.com", "secret");

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    assertThat(result.accessTokenExpiresInSeconds()).isEqualTo(900L);

    verify(userRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                user ->
                    user.getUsername().equals("alice")
                        && user.getEmail().equals("alice@example.com")
                        && user.getPasswordHash().equals("hashed-secret")
                        && user.getRoles().equals(Set.of("USER"))
                        && user.isEnabled()));
  }

  @Test
  void register_throwsConflict_whenUsernameAlreadyTaken() {
    when(userRepository.existsByUsername("alice")).thenReturn(true);

    assertThatThrownBy(() -> authService.register("alice", "alice@example.com", "secret"))
        .isInstanceOf(ConflictException.class);

    verify(userRepository, never()).save(any());
  }

  @Test
  void login_issuesTokenPair_whenCredentialsAreValid() {
    User user =
        new User("u-1", "alice", "alice@example.com", "hashed-secret", Set.of("USER"), true);
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("secret", "hashed-secret")).thenReturn(true);
    when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
    when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
    when(refreshTokenStore.issue("u-1")).thenReturn("refresh-token");

    TokenPair result = authService.login("alice", "secret");

    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
  }

  @Test
  void login_throwsUnauthorized_whenUserNotFound() {
    when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login("alice", "secret"))
        .isInstanceOf(ApplicationException.class);
  }

  @Test
  void login_throwsUnauthorized_whenPasswordDoesNotMatch() {
    User user =
        new User("u-1", "alice", "alice@example.com", "hashed-secret", Set.of("USER"), true);
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "hashed-secret")).thenReturn(false);

    assertThatThrownBy(() -> authService.login("alice", "wrong"))
        .isInstanceOf(ApplicationException.class);

    verify(tokenProvider, never()).generateAccessToken(any());
  }

  @Test
  void login_throwsUnauthorized_whenUserDisabled() {
    User user =
        new User("u-1", "alice", "alice@example.com", "hashed-secret", Set.of("USER"), false);
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.login("alice", "secret"))
        .isInstanceOf(ApplicationException.class);
  }

  @Test
  void refresh_consumesOldTokenAtomicallyAndIssuesNewPair() {
    User user =
        new User("u-1", "alice", "alice@example.com", "hashed-secret", Set.of("USER"), true);
    when(refreshTokenStore.consume("old-refresh")).thenReturn(Optional.of("u-1"));
    when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
    when(tokenProvider.generateAccessToken(user)).thenReturn("new-access-token");
    when(tokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
    when(refreshTokenStore.issue("u-1")).thenReturn("new-refresh-token");

    TokenPair result = authService.refresh("old-refresh");

    assertThat(result.accessToken()).isEqualTo("new-access-token");
    assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    verify(refreshTokenStore, never()).revoke(any());
  }

  @Test
  void refresh_throwsUnauthorized_whenTokenInvalidOrExpired() {
    when(refreshTokenStore.consume("bad-token")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("bad-token"))
        .isInstanceOf(ApplicationException.class);
  }

  @Test
  void refresh_throwsUnauthorized_whenUserNoLongerExists() {
    when(refreshTokenStore.consume("old-refresh")).thenReturn(Optional.of("u-1"));
    when(userRepository.findById("u-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("old-refresh"))
        .isInstanceOf(ApplicationException.class);
  }

  @Test
  void refresh_throwsUnauthorized_whenUserDisabled() {
    User disabled =
        new User("u-1", "alice", "alice@example.com", "hashed-secret", Set.of("USER"), false);
    when(refreshTokenStore.consume("old-refresh")).thenReturn(Optional.of("u-1"));
    when(userRepository.findById("u-1")).thenReturn(Optional.of(disabled));

    assertThatThrownBy(() -> authService.refresh("old-refresh"))
        .isInstanceOf(ApplicationException.class);

    verify(tokenProvider, never()).generateAccessToken(any());
  }

  @Test
  void logout_revokesRefreshToken() {
    authService.logout("refresh-token");

    verify(refreshTokenStore).revoke("refresh-token");
  }
}
