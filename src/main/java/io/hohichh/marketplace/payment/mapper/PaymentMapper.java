package io.hohichh.marketplace.payment.mapper;

import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentDto;
import io.hohichh.marketplace.payment.model.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    Payment toEntity(NewPaymentDto newPaymentDto);
    Payment toEntity(PaymentDto paymentDto);
    PaymentDto toDto(Payment payment);
}
