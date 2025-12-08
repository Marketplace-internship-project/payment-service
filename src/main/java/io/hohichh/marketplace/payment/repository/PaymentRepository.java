package io.hohichh.marketplace.payment.repository;

import io.hohichh.marketplace.payment.dto.PaymentSumDto;
import io.hohichh.marketplace.payment.model.Payment;
import io.hohichh.marketplace.payment.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    Page<Payment> findByOrderId(String orderId, Pageable pageable);
    Page<Payment> findByUserId(String userId, Pageable pageable);
    Page<Payment> findByStatusIn(List<Status> statuses, Pageable pageable);

    @Aggregation(pipeline = {
            "{ '$match': {'timestamp' : { '$gte': ?0, '$lte': ?1 } } }",
            "{ '$group': {'_id': null, 'totalAmount' : { '$sum': '$paymentAmount' } } }"
    })
    Optional<PaymentSumDto> getTotalAmountByDateRange(LocalDateTime from, LocalDateTime to);
}
