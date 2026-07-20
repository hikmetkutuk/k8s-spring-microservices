package com.k8sspringmicroservices.common.security.jwt;

import com.k8sspringmicroservices.common.security.AuthenticatedUser;
import com.k8sspringmicroservices.common.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.PublicKey;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final PublicKey publicKey;

  public JwtAuthenticationFilter(PublicKey publicKey) {
    this.publicKey = publicKey;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

    if (header != null && header.startsWith(SecurityConstants.BEARER_PREFIX)) {
      String token = header.substring(SecurityConstants.BEARER_PREFIX.length());
      authenticate(token);
    }

    filterChain.doFilter(request, response);
  }

  private void authenticate(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();

      String userId = claims.get(SecurityConstants.USER_ID_CLAIM, String.class);
      @SuppressWarnings("unchecked")
      List<String> roles = claims.get(SecurityConstants.ROLES_CLAIM, List.class);

      List<GrantedAuthority> authorities =
          roles.stream()
              .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
              .map(GrantedAuthority.class::cast)
              .toList();

      AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, roles);
      var authentication =
          new UsernamePasswordAuthenticationToken(authenticatedUser, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("Invalid JWT token: {}", e.getMessage());
      SecurityContextHolder.clearContext();
    }
  }
}
