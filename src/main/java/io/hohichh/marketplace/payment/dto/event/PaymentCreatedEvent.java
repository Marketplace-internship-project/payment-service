package io.hohichh.marketplace.payment.dto.event;

import io.hohichh.marketplace.payment.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;


public record PaymentCreatedEvent(
        @NotBlank
        @NotNull
         String paymentId,

        @NotBlank
        @NotNull
         String orderId,

        @NotBlank
        @NotNull
        String userId,

         @NotNull
         Status status,

        @NotNull
         LocalDateTime timestamp
) implements Serializable {
}