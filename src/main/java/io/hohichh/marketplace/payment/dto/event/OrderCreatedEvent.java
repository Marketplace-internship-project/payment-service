package io.hohichh.marketplace.payment.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;


public record OrderCreatedEvent(
         @NotBlank
         @NotNull
         String orderId,

         @NotNull @NotBlank
         String userId,
         @NotNull
         BigDecimal amount) implements Serializable {

}