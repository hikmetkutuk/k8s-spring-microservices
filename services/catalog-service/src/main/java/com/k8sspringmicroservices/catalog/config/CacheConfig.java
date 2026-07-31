package com.k8sspringmicroservices.catalog.config;

import com.k8sspringmicroservices.catalog.domain.CatalogItem;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    // Varsayılan value serializer JDK serialization'dır ve CatalogItem bir record olduğu için
    // (Serializable değil) "Cannot serialize value ... without a serializer" hatası veriyordu —
    // gerçek cluster'da cache ilk kez uçtan uca denendiğinde ortaya çıktı.
    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    GenericJacksonJsonRedisSerializer.create(
                        builder ->
                            builder.enableDefaultTyping(
                                BasicPolymorphicTypeValidator.builder()
                                    .allowIfSubType(CatalogItem.class)
                                    .allowIfSubType(BigDecimal.class)
                                    .allowIfSubType(Instant.class)
                                    .allowIfSubType(List.class)
                                    .build()))));

    return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultConfig).build();
  }
}
