package com.logistics.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Multi-Threading ThreadPool configuration for asynchronous notification dispatch.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncNotificationThreadPoolConfig {

    public static final String NOTIF_DISPATCH_EXECUTOR = "notificationTaskExecutor";

    @Bean(name = NOTIF_DISPATCH_EXECUTOR)
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notif-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        log.info("Initialized Notification Dispatcher Thread Pool [Core: 10, Max: 30, Queue: 500]");
        return executor;
    }
}
