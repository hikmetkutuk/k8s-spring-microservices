package com.k8sspringmicroservices.auth.adapter.in.web;

import com.k8sspringmicroservices.auth.adapter.in.web.dto.LoginRequest;
import com.k8sspringmicroservices.auth.adapter.in.web.dto.RefreshRequest;
import com.k8sspringmicroservices.auth.adapter.in.web.dto.RegisterRequest;
import com.k8sspringmicroservices.auth.adapter.in.web.dto.TokenResponse;
import com.k8sspringmicroservices.auth.application.port.in.AuthUseCase;
import com.k8sspringmicroservices.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthUseCase authUseCase;

  public AuthController(AuthUseCase authUseCase) {
    this.authUseCase = authUseCase;
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<TokenResponse>> register(
      @Valid @RequestBody RegisterRequest request) {
    AuthUseCase.TokenPair tokenPair =
        authUseCase.register(request.username(), request.email(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(TokenResponse.from(tokenPair)));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    AuthUseCase.TokenPair tokenPair = authUseCase.login(request.username(), request.password());
    return ResponseEntity.ok(ApiResponse.of(TokenResponse.from(tokenPair)));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<TokenResponse>> refresh(
      @Valid @RequestBody RefreshRequest request) {
    AuthUseCase.TokenPair tokenPair = authUseCase.refresh(request.refreshToken());
    return ResponseEntity.ok(ApiResponse.of(TokenResponse.from(tokenPair)));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
    authUseCase.logout(request.refreshToken());
    return ResponseEntity.noContent().build();
  }
}
