package com.winten.greenlight.prototype.core.db.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.prototype.core.db.repository.redis.event.EventEntity;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.MicrometerTracing;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.List;

@Configuration
public class CoreRedisConfig {

    @Bean
    public ClientResources clientResources(ObservationRegistry observationRegistry) {
        return ClientResources.builder()
                .tracing(new MicrometerTracing(observationRegistry, "redis-service"))
                .build();
    }
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties properties, ClientResources clientResources) {
        var clusterNodes = properties.getCluster().getNodes();
        var clusterConfig = new RedisClusterConfiguration(clusterNodes);
        clusterConfig.setPassword(properties.getPassword());

        var topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(30)) // 주기적으로 토폴로지 새로고침
                .enableAllAdaptiveRefreshTriggers() // MOVED, ASK 등 다양한 트리거에 반응하여 새로고침
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(25)) // 적응형 새로고침 타임아웃
                .build();

        var clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(topologyRefreshOptions)
                .build();

        var clientConfig = LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .clientOptions(clusterClientOptions)
                .readFrom(ReadFrom.REPLICA_PREFERRED) // 읽기 작업을 슬레이브 노드에서 수행하도록 설정 (선택 사항)
                .commandTimeout(Duration.ofSeconds(10)) // 커맨드 타임아웃 설정
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    @Bean
    public ReactiveRedisOperations<String, String> reactiveRedisTemplate(LettuceConnectionFactory factory) {
        var serializer = new StringRedisSerializer();
        var serializationContext = RedisSerializationContext
                .<String, String>newSerializationContext()
                .key(serializer)
                .value(serializer)
                .hashKey(serializer)
                .hashValue(serializer)
                .build();
        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }

    @Bean
    public ReactiveRedisTemplate<String, EventEntity> reactiveEventRedisTemplate(LettuceConnectionFactory factory, ObjectMapper objectMapper) {
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        RedisSerializer<EventEntity> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, EventEntity.class);

        RedisSerializationContext<String, EventEntity> serializationContext = RedisSerializationContext
                .<String, EventEntity>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
}