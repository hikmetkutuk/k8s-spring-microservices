package com.k8sspringmicroservices.auth.adapter.out.token;

import com.k8sspringmicroservices.auth.application.port.out.TokenProviderPort;
import com.k8sspringmicroservices.auth.config.JwtProperties;
import com.k8sspringmicroservices.auth.domain.User;
import com.k8sspringmicroservices.common.security.SecurityConstants;
import com.k8sspringmicroservices.common.security.jwt.RsaKeyLoader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements TokenProviderPort {

  private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

  private final PrivateKey privateKey;
  private final PublicKey publicKey;
  private final JwtProperties jwtProperties;

  public JwtTokenProvider(RsaKeyLoader rsaKeyLoader, JwtProperties jwtProperties) {
    this.privateKey = rsaKeyLoader.loadPrivateKey(jwtProperties.getPrivateKeyLocation());
    this.publicKey = rsaKeyLoader.loadPublicKey(jwtProperties.getPublicKeyLocation());
    this.jwtProperties = jwtProperties;
  }

  @Override
  public String generateAccessToken(User user) {
    Instant now = Instant.now();
    Instant expiry = now.plusSeconds(jwtProperties.getAccessTokenTtlSeconds());

    return Jwts.builder()
        .subject(user.getId())
        .issuer(jwtProperties.getIssuer())
        .claim(SecurityConstants.USER_ID_CLAIM, user.getId())
        .claim(SecurityConstants.ROLES_CLAIM, user.getRoles())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  @Override
  public long getAccessTokenExpirationSeconds() {
    return jwtProperties.getAccessTokenTtlSeconds();
  }

  @Override
  public Optional<String> extractUserId(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
      return Optional.ofNullable(claims.getSubject());
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("Invalid JWT token: {}", e.getMessage());
      return Optional.empty();
    }
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }
}
