package com.k8sspringmicroservices.common.security;

import java.util.List;

public record AuthenticatedUser(String userId, List<String> roles) {

  public boolean hasRole(String role) {
    return roles.contains(role);
  }
}
