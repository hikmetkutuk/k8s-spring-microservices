package com.k8sspringmicroservices.common.security.jwt;

import java.security.PublicKey;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@EnableConfigurationProperties(JwtValidationProperties.class)
@ConditionalOnProperty(prefix = "jwt", name = "public-key-location")
@Import(RsaKeyLoader.class)
public class JwtSecurityAutoConfiguration {

  @Bean
  public PublicKey jwtPublicKey(RsaKeyLoader rsaKeyLoader, JwtValidationProperties properties) {
    return rsaKeyLoader.loadPublicKey(properties.getPublicKeyLocation());
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(PublicKey jwtPublicKey) {
    return new JwtAuthenticationFilter(jwtPublicKey);
  }
}
