package com.k8sspringmicroservices.auth.application.port.out;

import java.util.Optional;

public interface RefreshTokenStorePort {

  String issue(String userId);

  Optional<String> resolveUserId(String refreshToken);

  /**
   * Atomically resolves the userId and revokes the token in one operation. Prevents two concurrent
   * refresh requests from both succeeding with the same token.
   */
  Optional<String> consume(String refreshToken);

  void revoke(String refreshToken);
}
