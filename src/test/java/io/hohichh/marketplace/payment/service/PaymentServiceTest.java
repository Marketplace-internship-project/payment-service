package io.hohichh.marketplace.payment.service;

import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.kafka.PaymentProducer;
import io.hohichh.marketplace.payment.model.Status;
import io.hohichh.marketplace.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;


@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private Clock clock;
    private final LocalDateTime FREEZE_TIME = LocalDateTime.of(2020, 1, 1, 0, 0);

    @Mock
    private PaymentProducer paymentProducer;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private NewPaymentDto newPaymentDto;

    @BeforeEach
    void setUp() {
         newPaymentDto = new NewPaymentDto(
                "order_id_1",
                "user_id_1",
                Status.PENDING,
                FREEZE_TIME,
                BigDecimal.valueOf(9.99)
        );
    }

    @Test
    void createPayment_bankResponseSucceed_OddNumber(){

    }

    @Test
    void createPayment_bankResponseSucceed_EvenNumber(){

    }

    @Test
    void createPayment_bankResponseFailed(){

    }

    @Test
    void createPayment_throwConflictException(){

    }

}
