package com.k8sspringmicroservices.notification.adapter.in.web;

import com.k8sspringmicroservices.common.dto.ApiResponse;
import com.k8sspringmicroservices.common.security.AuthenticatedUser;
import com.k8sspringmicroservices.notification.adapter.in.web.dto.NotificationResponse;
import com.k8sspringmicroservices.notification.application.port.in.NotificationUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

  private final NotificationUseCase notificationUseCase;

  public NotificationController(NotificationUseCase notificationUseCase) {
    this.notificationUseCase = notificationUseCase;
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<List<NotificationResponse>>> listMyNotifications(
      @AuthenticationPrincipal AuthenticatedUser currentUser) {
    List<NotificationResponse> notifications =
        notificationUseCase.listByOwner(currentUser.userId()).stream()
            .map(NotificationResponse::from)
            .toList();
    return ResponseEntity.ok(ApiResponse.of(notifications));
  }
}
