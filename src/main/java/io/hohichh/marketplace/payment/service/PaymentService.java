package io.hohichh.marketplace.payment.service;

import io.hohichh.marketplace.payment.dto.PaymentSumDto;
import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentDto;
import io.hohichh.marketplace.payment.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {
    PaymentDto createPayment(NewPaymentDto payment);
    void deletePayment(String paymentId);
    PaymentDto changePaymentStatus(String paymentId, Status newStatus);
    Page<PaymentDto> getPaymentsByOrderId(String orderId, Pageable pageable);
    Page<PaymentDto> getPaymentsByUserId(String userId, Pageable pageable);
    Page<PaymentDto> getPaymentsByStatuses(List<Status> statuses, Pageable pageable);
    PaymentSumDto getTotalSumByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}
