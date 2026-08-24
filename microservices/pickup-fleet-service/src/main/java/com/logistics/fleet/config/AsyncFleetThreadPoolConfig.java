package com.logistics.fleet.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Multi-Threading ThreadPool configuration for Fleet Dispatch & Geo-Spatial Driver Search.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncFleetThreadPoolConfig {

    public static final String FLEET_MATCH_EXECUTOR = "fleetMatchingExecutor";

    @Bean(name = FLEET_MATCH_EXECUTOR)
    public Executor fleetMatchingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("fleet-matcher-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        log.info("Initialized Fleet Spatial Matcher Thread Pool [Core: 6, Max: 20, Queue: 200]");
        return executor;
    }
}
