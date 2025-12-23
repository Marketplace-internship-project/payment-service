package io.hohichh.marketplace.payment.controller;

import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentSumDto;
import io.hohichh.marketplace.payment.model.Status;
import io.hohichh.marketplace.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class RestPaymentController {

    private final PaymentService paymentService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentDto createPayment(@RequestBody @Valid NewPaymentDto newPaymentDto) {
        log.info("Received request to create payment");
        return paymentService.createPayment(newPaymentDto);
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable String id) {
        log.info("Received request to delete payment with ID: {}", id);
        paymentService.deletePayment(id);
    }


    @GetMapping("/by-order/{orderId}")
    public Page<PaymentDto> getPaymentsByOrderId(
            @PathVariable String orderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return paymentService.getPaymentsByOrderId(orderId, pageable);
    }

    @GetMapping("/by-user/{userId}")
    public Page<PaymentDto> getPaymentsByUserId(
            @PathVariable String userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return paymentService.getPaymentsByUserId(userId, pageable);
    }

    @GetMapping("/search")
    public Page<PaymentDto> getPaymentsByStatuses(
            @RequestParam List<Status> statuses,
            @PageableDefault(size = 20) Pageable pageable) {
        return paymentService.getPaymentsByStatuses(statuses, pageable);
    }

    // GET /api/payments/summary?from=2023-10-01T00:00:00&to=2023-10-31T23:59:59
    @GetMapping("/summary")
    public PaymentSumDto getSummaryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return paymentService.getTotalSumByDateRange(from, to);
    }
}