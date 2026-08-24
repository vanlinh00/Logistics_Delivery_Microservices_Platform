package com.logistics.notification.factory;

import com.logistics.notification.model.NotificationLog.Channel;
import com.logistics.notification.strategy.NotificationChannelStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory Pattern: Resolves appropriate NotificationChannelStrategy dynamically.
 * 
 * SOLID:
 * - Dependency Inversion: Injects List<NotificationChannelStrategy>.
 * - Open/Closed: New channel strategies are automatically registered without code edits.
 */
@Component
@Slf4j
public class NotificationStrategyFactory {

    private final Map<Channel, NotificationChannelStrategy> strategyMap = new EnumMap<>(Channel.class);

    public NotificationStrategyFactory(List<NotificationChannelStrategy> strategies) {
        for (NotificationChannelStrategy strategy : strategies) {
            strategyMap.put(strategy.getSupportedChannel(), strategy);
            log.info("Registered Notification Channel Strategy: [{}] for Channel [{}]",
                    strategy.getClass().getSimpleName(), strategy.getSupportedChannel());
        }
    }

    public NotificationChannelStrategy getStrategy(Channel channel) {
        return Optional.ofNullable(strategyMap.get(channel))
                .orElseGet(() -> {
                    log.warn("No specific strategy for channel {}, defaulting to SMS", channel);
                    return strategyMap.get(Channel.SMS);
                });
    }
}
