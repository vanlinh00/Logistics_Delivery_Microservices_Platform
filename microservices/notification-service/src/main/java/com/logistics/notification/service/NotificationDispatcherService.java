package com.logistics.notification.service;

import com.logistics.notification.factory.NotificationStrategyFactory;
import com.logistics.notification.model.NotificationLog;
import com.logistics.notification.repository.NotificationLogRepository;
import com.logistics.notification.strategy.NotificationChannelStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service orchestrating notification dispatching with Strategy and Factory patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcherService {

    private final NotificationStrategyFactory strategyFactory;
    private final NotificationLogRepository repository;

    @Transactional
    public NotificationLog dispatch(NotificationLog notif) {
        NotificationChannelStrategy strategy = strategyFactory.getStrategy(notif.getChannel());
        
        boolean sent = false;
        try {
            sent = strategy.sendNotification(
                    notif.getRecipient(),
                    notif.getTitle(),
                    notif.getMessageContent(),
                    notif.getTrackingNumber()
            );
        } catch (Exception e) {
            log.error("Failed to send notification via [{}]: {}", notif.getChannel(), e.getMessage());
        }

        notif.setStatus(sent ? NotificationLog.Status.SENT : NotificationLog.Status.FAILED);
        return repository.save(notif);
    }
}
