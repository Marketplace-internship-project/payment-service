package io.hohichh.marketplace.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;

    @Indexed
    @Field("order_id")
    private String orderId;

    @Indexed
    @Field("user_id")
    private String userId;

    @Indexed
    private Status status;

    @Indexed
    private LocalDateTime timestamp;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;
}
