package com.logistics.tracking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Multi-Threading ThreadPool configuration for Tracking query aggregation and async indexing.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncTrackingThreadPoolConfig {

    public static final String TRACKING_EXECUTOR = "trackingTaskExecutor";

    @Bean(name = TRACKING_EXECUTOR)
    public Executor trackingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(25);
        executor.setQueueCapacity(300);
        executor.setThreadNamePrefix("tracking-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        log.info("Initialized Tracking Multi-Threading Thread Pool [Core: 8, Max: 25, Queue: 300]");
        return executor;
    }
}
