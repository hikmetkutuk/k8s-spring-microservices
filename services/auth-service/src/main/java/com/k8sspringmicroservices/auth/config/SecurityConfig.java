package com.k8sspringmicroservices.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@SuppressWarnings("java:S4502")
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) {
    // Stateless JWT bearer-token API'de session/cookie kullanılmadığı için CSRF riski yok.
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // X-Frame-Options DENY ve X-Content-Type-Options nosniff zaten Spring Security
        // varsayılanı; CSP ve HSTS ise defense-in-depth için burada da açıkça ekleniyor
        // (asıl edge api-gateway'in SecureHeaders filter'ı — bkz. api-gateway application.yml).
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                    .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/auth/**",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }
}
