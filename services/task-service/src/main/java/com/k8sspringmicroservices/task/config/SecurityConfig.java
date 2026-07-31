package com.k8sspringmicroservices.task.config;

import com.k8sspringmicroservices.common.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) {
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
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/prometheus",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
