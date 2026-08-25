package com.logistics.order.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson() {
        Config config = new Config();
        var singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        if (StringUtils.hasText(redisPassword)) {
            singleServerConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}
