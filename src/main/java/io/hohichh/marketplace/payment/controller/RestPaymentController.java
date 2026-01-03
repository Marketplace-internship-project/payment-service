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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class RestPaymentController {

    private final PaymentService paymentService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("#newPaymentDto.userId == authentication.principal")
    public PaymentDto createPayment(@RequestBody @Valid NewPaymentDto newPaymentDto) {
        log.info("Received request to create payment");
        return paymentService.createPayment(newPaymentDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePayment(@PathVariable String id) {
        log.info("Received request to delete payment with ID: {}", id);
        paymentService.deletePayment(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentDto updatePaymentStatus(@PathVariable String id,
                                          @RequestParam Status status){
        log.info("Received request to update payment with ID: {}", id);
        return paymentService.changePaymentStatus(id, status);
    }


    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or (#userId != null and #userId == authentication.principal)")
    public Page<PaymentDto> searchPayments(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) List<Status> statuses,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Searching payments with params: userId={}, orderId={}, statuses={}", userId, orderId, statuses);


        return paymentService.findPayments(userId, orderId, statuses, pageable);
    }



    // GET /api/payments/summary?from=2023-10-01T00:00:00&to=2023-10-31T23:59:59
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentSumDto getSummaryByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return paymentService.getTotalSumByDateRange(from, to);
    }
}