package com.k8sspringmicroservices.gateway.config;

import com.k8sspringmicroservices.common.security.jwt.JwtValidationProperties;
import com.k8sspringmicroservices.common.security.jwt.RsaKeyLoader;
import java.security.PublicKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtValidationProperties.class, GatewaySecurityProperties.class})
public class GatewaySecurityConfig {

  @Bean
  public PublicKey jwtPublicKey(JwtValidationProperties properties) {
    return new RsaKeyLoader().loadPublicKey(properties.getPublicKeyLocation());
  }
}
