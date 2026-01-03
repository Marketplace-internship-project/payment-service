package io.hohichh.marketplace.payment.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;


import java.io.Serializable;
import java.math.BigDecimal;

public record PaymentSumDto(
        @Digits(integer = 20, fraction = 2)
        @NotNull
        BigDecimal totalAmount
) implements Serializable {
}
