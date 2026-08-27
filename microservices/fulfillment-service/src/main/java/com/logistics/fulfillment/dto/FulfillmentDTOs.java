package com.logistics.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.logistics.fulfillment.model.HubTransitRecord;
import com.logistics.fulfillment.model.ProofOfDelivery;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

public final class FulfillmentDTOs {

    private FulfillmentDTOs() {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PodSubmitRequest {

        @NotBlank(message = "Tracking number must not be blank")
        private String trackingNumber;

        private UUID orderId;

        @NotBlank(message = "Recipient signed name must not be blank")
        private String recipientSignedName;

        private String recipientPhone;

        @JsonAlias({"signatureImageUrl", "signature"})
        private String signatureDataUri;

        @JsonAlias({"photoDeliveryUrl", "photoUrl"})
        private String photoEvidenceUrl;

        private Double deliveryLatitude;
        private Double deliveryLongitude;

        @JsonAlias({"driverId"})
        private String courierId;

        @JsonAlias({"notes", "deliveryNotes"})
        private String courierNotes;

        @Builder.Default
        private ProofOfDelivery.DeliveryResult result = ProofOfDelivery.DeliveryResult.SUCCESS;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HubScanRequest {

        @NotBlank(message = "Tracking number must not be blank")
        private String trackingNumber;

        @NotBlank(message = "Source hub ID must not be blank")
        private String sourceHubId;

        @JsonAlias({"targetHubId"})
        private String destinationHubId;

        private String vehiclePlate;

        @JsonAlias({"sealNumber"})
        private String containerSealNumber;

        @Builder.Default
        private HubTransitRecord.TransitStatus status = HubTransitRecord.TransitStatus.SORTED_AT_ORIGIN;

        private String operatorId;
    }
}
