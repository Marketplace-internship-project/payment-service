package io.hohichh.marketplace.payment.kafka;

import io.hohichh.marketplace.payment.dto.event.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentStatus(PaymentCreatedEvent event) {
        log.info("Sending payment created event for Order: {}, Status: {}", event.orderId(), event.status());

        kafkaTemplate.send("payment-events", event.orderId(), event);
    }
}