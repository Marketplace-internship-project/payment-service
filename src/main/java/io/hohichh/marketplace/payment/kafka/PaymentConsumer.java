package io.hohichh.marketplace.payment.kafka;

import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.dto.event.OrderCreatedEvent;
import io.hohichh.marketplace.payment.model.Status;
import io.hohichh.marketplace.payment.service.PaymentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentConsumer {

    private final PaymentServiceImpl paymentService;
    private final Clock clock;

    @KafkaListener(topics = "order-created-events", groupId = "payment-service-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId: {}", event.orderId());
        paymentService.createPayment(new NewPaymentDto(
                event.orderId(),
                event.userId(),
                Status.PENDING,
                LocalDateTime.now(clock),
                event.amount()
                ));
    }
}