package io.hohichh.marketplace.payment.service;

import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentSumDto;
import io.hohichh.marketplace.payment.model.Status;
import io.hohichh.marketplace.payment.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentDto createPayment(NewPaymentDto payment) {
        return null;
    }

    @Override
    public void deletePayment(String paymentId) {

    }

    @Override
    public Page<PaymentDto> getPaymentsByOrderId(String orderId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<PaymentDto> getPaymentsByUserId(String userId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<PaymentDto> getPaymentsByStatuses(List<Status> statuses, Pageable pageable) {
        return null;
    }

    @Override
    public PaymentSumDto getTotalSumByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return null;
    }
}
