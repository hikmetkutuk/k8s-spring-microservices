package com.k8sspringmicroservices.common.security;

public final class SecurityConstants {

  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String BEARER_PREFIX = "Bearer ";
  public static final String USER_ID_CLAIM = "userId";
  public static final String ROLES_CLAIM = "roles";
  public static final String USER_ID_HEADER = "X-User-Id";
  public static final String USER_ROLES_HEADER = "X-User-Roles";

  private SecurityConstants() {}
}
