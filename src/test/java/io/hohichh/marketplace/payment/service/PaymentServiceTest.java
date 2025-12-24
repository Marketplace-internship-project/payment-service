package io.hohichh.marketplace.payment.service;

import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentSumDto;
import io.hohichh.marketplace.payment.dto.event.PaymentCreatedEvent;
import io.hohichh.marketplace.payment.exception.ActionNotPermittedException;
import io.hohichh.marketplace.payment.exception.ResourceCreationConflictException;
import io.hohichh.marketplace.payment.exception.ResourceNotFoundException;
import io.hohichh.marketplace.payment.kafka.PaymentProducer;
import io.hohichh.marketplace.payment.mapper.PaymentMapper;
import io.hohichh.marketplace.payment.model.Payment;
import io.hohichh.marketplace.payment.model.Status;
import io.hohichh.marketplace.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private Clock clock;
    private final LocalDateTime FREEZE_TIME = LocalDateTime.of(2025, 1, 1, 12, 0);

    @Mock
    private PaymentProducer paymentProducer;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private NewPaymentDto newPaymentDto;
    private Payment paymentEntity;
    private final String MOCK_API_URL = "http://mock-bank-api.com";

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(paymentService, "externalApiUrl", MOCK_API_URL);

        newPaymentDto = new NewPaymentDto(
                "order_123",
                "user_456",
                Status.PENDING,
                FREEZE_TIME,
                new BigDecimal("100.00")
        );


        paymentEntity = new Payment();
        paymentEntity.setId("payment_uuid_1");
        paymentEntity.setOrderId("order_123");
        paymentEntity.setUserId("user_456");
        paymentEntity.setPaymentAmount(new BigDecimal("100.00"));
        paymentEntity.setTimestamp(FREEZE_TIME);
    }

    @Test
    void createPayment_bankResponseSucceed_EvenNumber() {
        when(paymentMapper.toEntity(newPaymentDto)).thenReturn(paymentEntity);
        when(paymentRepository.existsById(paymentEntity.getId())).thenReturn(false);
        when(restTemplate.getForObject(MOCK_API_URL, Integer[].class)).thenReturn(new Integer[]{42});
        when(paymentRepository.save(any(Payment.class))).thenReturn(paymentEntity);


        PaymentDto expectedDto = new PaymentDto(
                paymentEntity.getId(), paymentEntity.getOrderId(), paymentEntity.getUserId(),
                Status.SUCCEED, FREEZE_TIME, paymentEntity.getPaymentAmount()
        );
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(expectedDto);


        PaymentDto result = paymentService.createPayment(newPaymentDto);


        assertNotNull(result);
        assertEquals(Status.SUCCEED, result.status());


        verify(paymentProducer).sendPaymentCreatedEvent(any(PaymentCreatedEvent.class));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(Status.SUCCEED, paymentCaptor.getValue().getStatus());
    }

    @Test
    void createPayment_bankResponseSucceed_OddNumber() {

        when(paymentMapper.toEntity(newPaymentDto)).thenReturn(paymentEntity);
        when(paymentRepository.existsById(paymentEntity.getId())).thenReturn(false);


        when(restTemplate.getForObject(MOCK_API_URL, Integer[].class)).thenReturn(new Integer[]{11});

        when(paymentRepository.save(any(Payment.class))).thenReturn(paymentEntity);

        PaymentDto expectedDto = new PaymentDto(
                paymentEntity.getId(), paymentEntity.getOrderId(), paymentEntity.getUserId(),
                Status.DECLINED, FREEZE_TIME, paymentEntity.getPaymentAmount()
        );
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(expectedDto);


        PaymentDto result = paymentService.createPayment(newPaymentDto);


        assertEquals(Status.DECLINED, result.status());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(Status.DECLINED, paymentCaptor.getValue().getStatus());
    }

    @Test
    void createPayment_bankResponseFailed() {

        when(paymentMapper.toEntity(newPaymentDto)).thenReturn(paymentEntity);
        when(paymentRepository.existsById(paymentEntity.getId())).thenReturn(false);


        when(restTemplate.getForObject(MOCK_API_URL, Integer[].class))
                .thenThrow(new RestClientException("Service Unavailable"));

        when(paymentRepository.save(any(Payment.class))).thenReturn(paymentEntity);

        PaymentDto expectedDto = new PaymentDto(
                paymentEntity.getId(), paymentEntity.getOrderId(), paymentEntity.getUserId(),
                Status.DECLINED, FREEZE_TIME, paymentEntity.getPaymentAmount()
        );
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(expectedDto);

        PaymentDto result = paymentService.createPayment(newPaymentDto);

        assertEquals(Status.DECLINED, result.status());
    }

    @Test
    void createPayment_throwConflictException() {
        when(paymentMapper.toEntity(newPaymentDto)).thenReturn(paymentEntity);
        when(paymentRepository.existsById(paymentEntity.getId())).thenReturn(true);

        assertThrows(ResourceCreationConflictException.class, () -> {
            paymentService.createPayment(newPaymentDto);
        });

        verify(paymentRepository, never()).save(any());
        verify(paymentProducer, never()).sendPaymentCreatedEvent(any());
    }

    @Test
    void deletePayment_succeed() {
        String paymentId = "payment_uuid_1";
        when(paymentRepository.existsById(paymentId)).thenReturn(true);

        paymentService.deletePayment(paymentId);

        verify(paymentRepository).deleteById(paymentId);
    }

    @Test
    void deletePayment_throwResourceNotFoundException() {
        String paymentId = "non_existent_id";
        when(paymentRepository.existsById(paymentId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.deletePayment(paymentId)
        );

        verify(paymentRepository, never()).deleteById(any());
    }

    @Test
    void changePaymentStatus_succeed() {
        String paymentId = "payment_uuid_1";
        Status newStatus = Status.REFUNDED;

        Payment existingPayment = Payment.builder()
                .id(paymentId)
                .status(Status.SUCCEED)
                .build();

        Payment updatedPayment = Payment.builder()
                .id(paymentId)
                .status(newStatus)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(java.util.Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(updatedPayment);

        PaymentDto expectedDto = new PaymentDto(
                paymentId, "order_1", "user_1", newStatus, FREEZE_TIME, BigDecimal.TEN
        );
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(expectedDto);

        PaymentDto result = paymentService.changePaymentStatus(paymentId, newStatus);

        assertEquals(newStatus, result.status());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(newStatus, paymentCaptor.getValue().getStatus());
    }

    @Test
    void changePaymentStatus_throwResourceNotFoundException() {
        String paymentId = "non_existent_id";
        when(paymentRepository.findById(paymentId)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                paymentService.changePaymentStatus(paymentId, Status.SUCCEED)
        );

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void changePaymentStatus_throwActionNotPermittedException() {
        String paymentId = "payment_uuid_1";

        Payment declinedPayment = Payment.builder()
                .id(paymentId)
                .status(Status.DECLINED)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(java.util.Optional.of(declinedPayment));

        assertThrows(ActionNotPermittedException.class, () ->
                paymentService.changePaymentStatus(paymentId, Status.SUCCEED)
        );

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getPaymentsByOrderId_shouldReturnPageOfDtos() {
        String orderId = "order_123";
        Pageable pageable = PageRequest.of(0, 10);

        List<Payment> paymentList = Collections.singletonList(paymentEntity);
        Page<Payment> paymentPage = new PageImpl<>(paymentList);

        when(paymentRepository.findByOrderId(orderId, pageable)).thenReturn(paymentPage);

        PaymentDto expectedDto = new PaymentDto(
                paymentEntity.getId(), paymentEntity.getOrderId(), paymentEntity.getUserId(),
                paymentEntity.getStatus(), paymentEntity.getTimestamp(), paymentEntity.getPaymentAmount()
        );
        when(paymentMapper.toDto(paymentEntity)).thenReturn(expectedDto);

        Page<PaymentDto> result = paymentService.getPaymentsByOrderId(orderId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedDto, result.getContent().get(0));

        verify(paymentRepository).findByOrderId(orderId, pageable);
    }

    @Test
    void getPaymentsByOrderId_shouldReturnEmptyPage_whenNoPaymentsFound() {
        String orderId = "unknown_order";
        Pageable pageable = PageRequest.of(0, 10);

        Page<Payment> emptyPage = Page.empty();
        when(paymentRepository.findByOrderId(orderId, pageable)).thenReturn(emptyPage);
        Page<PaymentDto> result = paymentService.getPaymentsByOrderId(orderId, pageable);

        assertTrue(result.isEmpty());
        verify(paymentRepository).findByOrderId(orderId, pageable);
        verify(paymentMapper, never()).toDto(any());
    }

    @Test
    void getPaymentsByUserId_shouldReturnPageOfDtos() {

        String userId = "user_456";
        Pageable pageable = PageRequest.of(0, 5);

        List<Payment> paymentList = Collections.singletonList(paymentEntity);
        Page<Payment> paymentPage = new PageImpl<>(paymentList);

        when(paymentRepository.findByUserId(userId, pageable)).thenReturn(paymentPage);

        PaymentDto expectedDto = new PaymentDto(
                paymentEntity.getId(), paymentEntity.getOrderId(), userId,
                paymentEntity.getStatus(), paymentEntity.getTimestamp(), paymentEntity.getPaymentAmount()
        );
        when(paymentMapper.toDto(paymentEntity)).thenReturn(expectedDto);

        Page<PaymentDto> result = paymentService.getPaymentsByUserId(userId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(userId, result.getContent().get(0).userId());
        verify(paymentRepository).findByUserId(userId, pageable);
    }

    @Test
    void getPaymentsByStatuses_shouldReturnPage_whenPaymentsExist() {
        List<Status> statuses = List.of(Status.PENDING, Status.SUCCEED);
        Pageable pageable = PageRequest.of(0, 20);

        List<Payment> paymentList = Collections.singletonList(paymentEntity);
        Page<Payment> paymentPage = new PageImpl<>(paymentList);

        when(paymentRepository.findByStatusIn(statuses, pageable)).thenReturn(paymentPage);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(mock(PaymentDto.class));
        Page<PaymentDto> result = paymentService.getPaymentsByStatuses(statuses, pageable);

        assertEquals(1, result.getTotalElements());
        verify(paymentRepository).findByStatusIn(statuses, pageable);
    }


    @Test
    void getTotalSumByDateRange_shouldReturnSum_whenDataExists() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        BigDecimal expectedSum = new BigDecimal("500.00");

        PaymentSumDto repoResult = new PaymentSumDto(expectedSum);

        when(paymentRepository.getTotalAmountByDateRange(start, end))
                .thenReturn(Optional.of(repoResult));

        PaymentSumDto result = paymentService.getTotalSumByDateRange(start, end);

        assertNotNull(result);
        assertEquals(expectedSum, result.totalAmount());
        verify(paymentRepository).getTotalAmountByDateRange(start, end);
    }

    @Test
    void getTotalSumByDateRange_shouldReturnZero_whenNoPaymentsFound() {
        LocalDateTime start = LocalDateTime.now().minusDays(10);
        LocalDateTime end = LocalDateTime.now().minusDays(9);

        when(paymentRepository.getTotalAmountByDateRange(start, end))
                .thenReturn(Optional.empty());

        PaymentSumDto result = paymentService.getTotalSumByDateRange(start, end);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.totalAmount());
        verify(paymentRepository).getTotalAmountByDateRange(start, end);
    }

}