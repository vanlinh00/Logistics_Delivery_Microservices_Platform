package com.logistics.notification.service;

import com.logistics.notification.constant.MessageCode;
import com.logistics.notification.dto.NotificationDTOs;
import com.logistics.notification.exception.ResourceNotFoundException;
import com.logistics.notification.model.NotificationLog;
import com.logistics.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository repository;
    private final NotificationDispatcherService dispatcherService;
    private final ParallelMultiChannelNotificationService parallelNotificationService;
    private final MessageService messageService;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLog> getLogs(String recipient, String trackingNumber) {
        if (recipient != null && !recipient.isBlank()) {
            return repository.findByRecipientOrderBySentAtDesc(recipient);
        }
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            return repository.findByTrackingNumber(trackingNumber);
        }
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationLog getLogById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessageCode.NOTIFICATION_NOT_FOUND,
                        messageService.getMessage(MessageCode.NOTIFICATION_NOT_FOUND, id.toString())
                ));
    }

    @Override
    @Transactional
    public NotificationLog sendManual(NotificationDTOs.SendNotificationRequest request) {
        NotificationLog.Channel channel = request.getChannel() != null 
                ? request.getChannel() 
                : NotificationLog.Channel.SMS;

        NotificationLog logEntry = NotificationLog.builder()
                .recipient(request.getRecipient())
                .channel(channel)
                .title(request.getTitle())
                .messageContent(request.getContent())
                .trackingNumber(request.getTrackingNumber())
                .build();

        return dispatcherService.dispatch(logEntry);
    }

    @Override
    @Transactional
    public NotificationLog sendManual(NotificationLog logEntry) {
        if (logEntry.getChannel() == null) {
            logEntry.setChannel(NotificationLog.Channel.SMS);
        }
        return dispatcherService.dispatch(logEntry);
    }

    @Override
    public List<NotificationLog> broadcastParallel(NotificationDTOs.BroadcastRequest request) {
        List<NotificationLog.Channel> channels = request.getChannels() != null && !request.getChannels().isEmpty()
                ? request.getChannels()
                : List.of(
                        NotificationLog.Channel.EMAIL,
                        NotificationLog.Channel.SMS,
                        NotificationLog.Channel.ZALO_ZNS,
                        NotificationLog.Channel.PUSH
                );

        return parallelNotificationService.broadcastConcurrently(
                channels,
                request.getRecipient(),
                request.getTitle(),
                request.getContent(),
                request.getTrackingNumber()
        );
    }
}
