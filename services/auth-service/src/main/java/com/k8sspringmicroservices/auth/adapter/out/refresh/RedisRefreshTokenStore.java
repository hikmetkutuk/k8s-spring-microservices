package com.k8sspringmicroservices.auth.adapter.out.refresh;

import com.k8sspringmicroservices.auth.application.port.out.RefreshTokenStorePort;
import com.k8sspringmicroservices.auth.config.JwtProperties;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisRefreshTokenStore implements RefreshTokenStorePort {

  private static final String KEY_PREFIX = "refresh-token:";

  private final StringRedisTemplate redisTemplate;
  private final JwtProperties jwtProperties;

  public RedisRefreshTokenStore(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
    this.redisTemplate = redisTemplate;
    this.jwtProperties = jwtProperties;
  }

  @Override
  public String issue(String userId) {
    String token = UUID.randomUUID().toString();
    redisTemplate
        .opsForValue()
        .set(
            KEY_PREFIX + token,
            userId,
            Duration.ofSeconds(jwtProperties.getRefreshTokenTtlSeconds()));
    return token;
  }

  @Override
  public Optional<String> resolveUserId(String refreshToken) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + refreshToken));
  }

  @Override
  public Optional<String> consume(String refreshToken) {
    return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + refreshToken));
  }

  @Override
  public void revoke(String refreshToken) {
    redisTemplate.delete(KEY_PREFIX + refreshToken);
  }
}
