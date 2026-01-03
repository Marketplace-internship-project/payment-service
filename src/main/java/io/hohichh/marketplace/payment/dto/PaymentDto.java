package io.hohichh.marketplace.payment.dto;

import io.hohichh.marketplace.payment.model.Status;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDto(
        @NotBlank
        @NotNull
        String id,

        @NotBlank
        @NotNull
        String orderId,

        @NotNull
        @NotBlank
        String userId,

        @NotNull
        Status status,

        @NotNull
        LocalDateTime timestamp,

        @NotNull
        @Digits(integer = 20, fraction = 2)
        BigDecimal paymentAmount
) implements Serializable{
}
