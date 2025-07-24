package com.winten.greenlight.prototype.core.db.config;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class CoreRedisConfig {
    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory(RedisProperties properties, ClientResources clientResources) {
        var clusterNodes = properties.getCluster().getNodes();
        var clusterConfig = new RedisClusterConfiguration(clusterNodes);
        clusterConfig.setPassword(properties.getPassword());

        var topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(30)) // 주기적으로 토폴로지 새로고침
                .enableAllAdaptiveRefreshTriggers() // MOVEC, ASK 등 트리거에 반응하여 새로고침
                .adaptiveRefreshTriggersTimeout(Duration.ofSeconds(25)) // 적응형 새로고침 타임아웃
                .build();

        var clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(topologyRefreshOptions)
                .build();

        var clientConfig = LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .clientOptions(clusterClientOptions)
                .readFrom(ReadFrom.REPLICA_PREFERRED) // 읽기 작업을 슬레이브 노드에서 수행하도록 설정
                .commandTimeout(Duration.ofSeconds(10)) // 커맨드 타임아웃 설정
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}