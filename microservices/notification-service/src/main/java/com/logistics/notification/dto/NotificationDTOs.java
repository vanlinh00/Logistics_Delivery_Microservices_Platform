package com.logistics.notification.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.logistics.notification.model.NotificationLog;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class NotificationDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendNotificationRequest {
        @NotBlank(message = "Recipient cannot be blank")
        private String recipient;

        private NotificationLog.Channel channel;

        @NotBlank(message = "Title cannot be blank")
        private String title;

        @NotBlank(message = "Content cannot be blank")
        @JsonAlias({"messageContent", "message", "body"})
        private String content;

        private String trackingNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastRequest {
        private List<NotificationLog.Channel> channels;

        @NotBlank(message = "Recipient cannot be blank")
        private String recipient;

        @NotBlank(message = "Title cannot be blank")
        private String title;

        @NotBlank(message = "Content cannot be blank")
        @JsonAlias({"messageContent", "message", "body"})
        private String content;

        private String trackingNumber;
    }
}
