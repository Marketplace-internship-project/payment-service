package io.hohichh.marketplace.payment.service;

import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentSumDto;
import io.hohichh.marketplace.payment.exception.ResourceNotFoundException;
import io.hohichh.marketplace.payment.kafka.PaymentProducer;
import io.hohichh.marketplace.payment.mapper.PaymentMapper;
import io.hohichh.marketplace.payment.model.Payment;
import io.hohichh.marketplace.payment.model.Status;
import io.hohichh.marketplace.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final Clock clock;
    private final RestTemplate restTemplate;

    @Value("${payment.external-bank-api-url}")
    private String externalApiUrl;

    private final PaymentProducer paymentProducer;

    @Override
    @Transactional
    public PaymentDto createPayment(NewPaymentDto newPaymentDto) {
        log.info("Request to create payment for orderId: {}, userId: {}, amount: {}",
                newPaymentDto.orderId(), newPaymentDto.userId(), newPaymentDto.paymentAmount());

        Payment payment = paymentMapper.toEntity(newPaymentDto);
        payment.setTimestamp(LocalDateTime.now(clock));

        boolean isSuccess;
        try {
            log.debug("Calling external bank API at: {}", externalApiUrl);
            Integer[] response = restTemplate.getForObject(externalApiUrl, Integer[].class);

            if (response != null && response.length > 0) {
                int randomNumber = response[0];
                isSuccess = (randomNumber % 2 == 0);
                log.info("External API returned: {}. Payment success: {}", randomNumber, isSuccess);
            } else {
                log.warn("External API returned empty response. Fallback to failure.");
                isSuccess = false;
            }
        } catch (RestClientException e) {
            log.error("Failed to call external API: {}. Using fallback logic.", e.getMessage());
            isSuccess = false;
        }

        Status status = isSuccess ? Status.SUCCEED : Status.DECLINED;
        payment.setStatus(status);
        //todo notify order service
        log.debug("Bank mock check result. Generated status: {}", status);

        Payment savedPayment = paymentRepository.save(payment);

        log.info("Payment created successfully. ID: {}, Status: {}", savedPayment.getId(), savedPayment.getStatus());

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    @Transactional
    public void deletePayment(String paymentId) {
        log.info("Request to delete payment with ID: {}", paymentId);
        if(!paymentRepository.existsById(paymentId)){
            log.error("Payment with ID: {} not found.", paymentId);
            throw new ResourceNotFoundException("Payment with ID: " + paymentId + " not found.");
        }
        paymentRepository.deleteById(paymentId);
        log.info("Payment with ID: {} processed for deletion", paymentId);
    }


    @Transactional
    @Override
    public PaymentDto changePaymentStatus(String paymentId, Status newStatus) {
        log.info("Changing status for payment {} to {}", paymentId, newStatus);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() == Status.DECLINED && newStatus == Status.SUCCEED) {
            throw new IllegalArgumentException("Cannot succeed a declined payment");
        }

        payment.setStatus(newStatus);
        Payment saved = paymentRepository.save(payment);
        //todo notify order service about refunding/cancelling

        return paymentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public Page<PaymentDto> getPaymentsByOrderId(String orderId, Pageable pageable) {
        log.debug("Fetching payments by orderId: {}", orderId);
        Page<Payment> payments = paymentRepository.findByOrderId(orderId, pageable);
        log.debug("Found {} payments for orderId: {}", payments.getTotalElements(), orderId);
        return payments.map(paymentMapper::toDto);
    }

    @Override
    public Page<PaymentDto> getPaymentsByUserId(String userId, Pageable pageable) {
        log.debug("Fetching payments by userId: {}", userId);
        Page<Payment> payments = paymentRepository.findByUserId(userId, pageable);
        log.debug("Found {} payments for userId: {}", payments.getTotalElements(), userId);
        return payments.map(paymentMapper::toDto);
    }

    @Override
    @Transactional
    public Page<PaymentDto> getPaymentsByStatuses(List<Status> statuses, Pageable pageable) {
        log.debug("Fetching payments by statuses: {}", statuses);
        Page<Payment> payments = paymentRepository.findByStatusIn(statuses, pageable);
        log.debug("Found {} payments for statuses: {}", payments.getTotalElements(), statuses);
        return payments.map(paymentMapper::toDto);
    }

    @Override
    @Transactional
    public PaymentSumDto getTotalSumByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating total sum between {} and {}", startDate, endDate);

        return paymentRepository.getTotalAmountByDateRange(startDate, endDate)
                .map(dto -> {
                    log.info("Total sum calculated: {}", dto.totalAmount());
                    return dto;
                })
                .orElseGet(() -> {
                    log.info("No payments found for period. Returning 0.");
                    return new PaymentSumDto(BigDecimal.ZERO);
                });
    }
}