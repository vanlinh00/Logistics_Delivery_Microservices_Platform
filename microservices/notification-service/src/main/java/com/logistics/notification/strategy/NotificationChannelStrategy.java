package com.logistics.notification.strategy;

import com.logistics.notification.model.NotificationLog.Channel;

/**
 * Strategy Pattern Interface for multi-channel message dispatching.
 *
 * Demonstrates SOLID:
 * - Single Responsibility Principle (SRP): Each strategy is only responsible for formatting and sending via its dedicated channel.
 * - Open/Closed Principle (OCP): New channels (e.g. Telegram, WhatsApp) can be added without changing existing classes.
 * - Liskov Substitution Principle (LSP): Any channel strategy can be used interchangeably by the dispatcher.
 */
public interface NotificationChannelStrategy {

    Channel getSupportedChannel();

    boolean sendNotification(String recipient, String title, String message, String trackingNumber);
}
