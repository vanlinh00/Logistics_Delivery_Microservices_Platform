package com.logistics.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Multi-Threading & Async Task Execution Configuration.
 * 
 * Configures bounded Thread Pools for:
 * 1. Outbox Event Dispatcher Worker Pool
 * 2. Parallel Shipping Rate Aggregator & Geocoding Pool
 * 3. General Asynchronous Domain Tasks
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncThreadPoolConfig {

    public static final String OUTBOX_EXECUTOR = "outboxTaskExecutor";
    public static final String PRICING_EXECUTOR = "pricingTaskExecutor";
    public static final String GENERAL_ASYNC_EXECUTOR = "generalAsyncExecutor";

    @Bean(name = OUTBOX_EXECUTOR)
    public Executor outboxTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("outbox-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Initialized Outbox Task Executor [Core: 5, Max: 15, Queue: 200]");
        return executor;
    }

    @Bean(name = PRICING_EXECUTOR)
    public Executor pricingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pricing-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("Initialized Pricing Task Executor [Core: 4, Max: 10, Queue: 100]");
        return executor;
    }

    @Bean(name = GENERAL_ASYNC_EXECUTOR)
    public Executor generalAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("order-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("Initialized General Order Async Executor [Core: 8, Max: 20, Queue: 500]");
        return executor;
    }
}
