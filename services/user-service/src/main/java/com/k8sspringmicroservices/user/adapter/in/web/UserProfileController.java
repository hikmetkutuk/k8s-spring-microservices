package com.k8sspringmicroservices.user.adapter.in.web;

import com.k8sspringmicroservices.common.dto.ApiResponse;
import com.k8sspringmicroservices.common.exception.ApplicationException;
import com.k8sspringmicroservices.common.security.AuthenticatedUser;
import com.k8sspringmicroservices.user.adapter.in.web.dto.CreateProfileRequest;
import com.k8sspringmicroservices.user.adapter.in.web.dto.UpdateProfileRequest;
import com.k8sspringmicroservices.user.adapter.in.web.dto.UserProfileResponse;
import com.k8sspringmicroservices.user.application.port.in.UserProfileUseCase;
import com.k8sspringmicroservices.user.domain.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserProfileController {

  private final UserProfileUseCase userProfileUseCase;

  public UserProfileController(UserProfileUseCase userProfileUseCase) {
    this.userProfileUseCase = userProfileUseCase;
  }

  @PostMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileResponse>> createMyProfile(
      @AuthenticationPrincipal AuthenticatedUser currentUser,
      @Valid @RequestBody CreateProfileRequest request) {
    UserProfile profile =
        userProfileUseCase.createProfile(
            currentUser.userId(), request.displayName(), request.bio(), request.avatarUrl());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(UserProfileResponse.from(profile)));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
      @AuthenticationPrincipal AuthenticatedUser currentUser) {
    UserProfile profile = userProfileUseCase.getProfile(currentUser.userId());
    return ResponseEntity.ok(ApiResponse.of(UserProfileResponse.from(profile)));
  }

  @PutMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
      @AuthenticationPrincipal AuthenticatedUser currentUser,
      @Valid @RequestBody UpdateProfileRequest request) {
    UserProfile profile =
        userProfileUseCase.updateProfile(
            currentUser.userId(), request.displayName(), request.bio(), request.avatarUrl());
    return ResponseEntity.ok(ApiResponse.of(UserProfileResponse.from(profile)));
  }

  @DeleteMapping("/me")
  public ResponseEntity<Void> deleteMyProfile(
      @AuthenticationPrincipal AuthenticatedUser currentUser) {
    userProfileUseCase.deleteProfile(currentUser.userId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{userId}")
  public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
      @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable String userId) {
    if (!currentUser.userId().equals(userId) && !currentUser.hasRole("ADMIN")) {
      throw new ApplicationException(HttpStatus.FORBIDDEN, "Not allowed to view this profile");
    }

    UserProfile profile = userProfileUseCase.getProfile(userId);
    return ResponseEntity.ok(ApiResponse.of(UserProfileResponse.from(profile)));
  }
}
