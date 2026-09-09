package com.winten.greenlight.core.db.config;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class CoreRedisConfig {
    @Value("${redis.connection-type:standalone}")
    private String redisConnectionType;

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(
            DataRedisProperties properties,
            ClientResources clientResources
    ) {
        if ("standalone".equalsIgnoreCase(redisConnectionType)) {
            return redisStandaloneConnectionFactory(properties, clientResources);
        } else if ("cluster".equalsIgnoreCase(redisConnectionType)) {
            return redisClusterConnectionFactory(properties, clientResources);
        }
        throw new IllegalArgumentException("Unsupported RedisConnectionType: " + redisConnectionType);
    }

    private LettuceConnectionFactory redisStandaloneConnectionFactory(
            DataRedisProperties properties,
            ClientResources clientResources
    ) {
        var standaloneConfig = new RedisStandaloneConfiguration(
                properties.getHost(),
                properties.getPort()
        );
        standaloneConfig.setPassword(properties.getPassword());

        var clientConfig = LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .commandTimeout(Duration.ofSeconds(10))
                .build();

        return new LettuceConnectionFactory(standaloneConfig, clientConfig);
    }

    private LettuceConnectionFactory redisClusterConnectionFactory(DataRedisProperties properties, ClientResources clientResources) {
        if (properties.getCluster() == null || properties.getCluster().getNodes() == null) {
            throw new IllegalArgumentException("Redis Cluster Nodes are required when RedisConnectionType is set to Cluster mode");
        }
        var clusterNodes = properties.getCluster().getNodes();
        var clusterConfig = new RedisClusterConfiguration(clusterNodes);
        clusterConfig.setPassword(properties.getPassword());

        var topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(30)) // 주기적으로 토폴로지 새로고침
                .enableAllAdaptiveRefreshTriggers() // MOVEC, ASK 등 트리거에 반응하여 새로고침
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(25)) // 적응형 새로고침 타임아웃
                .dynamicRefreshSources(true)
                .closeStaleConnections(true)
                .build();

        var socketOptions = SocketOptions.builder()
                .keepAlive(true)
                .build();

        var clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(topologyRefreshOptions)
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(3)))
                .maxRedirects(3)
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .socketOptions(socketOptions)
                .build();

        var clientConfigBuilder = LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .clientOptions(clusterClientOptions)
                .readFrom(ReadFrom.MASTER)
                .commandTimeout(Duration.ofSeconds(3)); // 커맨드 타임아웃 설정

        if (properties.getSsl().isEnabled()) {
            clientConfigBuilder.useSsl();
        }

        return new LettuceConnectionFactory(clusterConfig, clientConfigBuilder.build());
    }
}