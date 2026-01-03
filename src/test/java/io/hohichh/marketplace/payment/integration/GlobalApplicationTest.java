package io.hohichh.marketplace.payment.integration;

import io.hohichh.marketplace.payment.dto.NewPaymentDto;
import io.hohichh.marketplace.payment.dto.PaymentDto;
import io.hohichh.marketplace.payment.dto.event.OrderCreatedEvent;
import io.hohichh.marketplace.payment.model.Payment;
import io.hohichh.marketplace.payment.model.Status;
import io.hohichh.marketplace.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class GlobalApplicationTest extends AbstractApplicationTest {

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@BeforeEach
	void cleanDb() {
		paymentRepository.deleteAll();
	}

	@Test
	@DisplayName("REST Flow: Create Payment -> Bank OK -> Save DB -> Return DTO")
	void shouldCreatePaymentSuccessfully_WhenBankApproves() {
		String userId = "user-" + UUID.randomUUID();
		String orderId = "order-" + UUID.randomUUID();
		BigDecimal amount = BigDecimal.valueOf(100.00);

		stubFor(get(urlPathMatching("/api/random.*"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("[42]")
						.withStatus(200)));

		NewPaymentDto requestDto = new NewPaymentDto(
				orderId,
				userId,
				Status.PENDING,
				LocalDateTime.now(),
				amount
		);

		HttpEntity<NewPaymentDto> request = new HttpEntity<>(requestDto, getAuthHeaders(userId, "USER"));

		ResponseEntity<PaymentDto> response = restTemplate.exchange(
				"/v1/payments",
				HttpMethod.POST,
				request,
				PaymentDto.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(Status.SUCCEED);
		assertThat(response.getBody().orderId()).isEqualTo(orderId);

		var paymentsInDb = paymentRepository.findByOrderId(orderId, null);
		assertThat(paymentsInDb.getContent()).hasSize(1);
		assertThat(paymentsInDb.getContent().get(0).getStatus()).isEqualTo(Status.SUCCEED);

		verify(1, getRequestedFor(urlPathMatching("/api/random.*")));
	}

	@Test
	@DisplayName("Kafka Flow: OrderCreatedEvent -> Consume -> Create Payment -> Save DB")
	void shouldCreatePayment_WhenOrderEventReceived() {

		String orderId = "kafka-order-" + UUID.randomUUID();
		String userId = "kafka-user-" + UUID.randomUUID();
		BigDecimal amount = BigDecimal.valueOf(55.50);

		stubFor(get(urlPathMatching("/api/random.*"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("[100]")
						.withStatus(200)));

		OrderCreatedEvent event = new OrderCreatedEvent(orderId, userId, amount);

		kafkaTemplate.send("order-created-events", orderId, event);

		await()
				.atMost(10, TimeUnit.SECONDS)
				.pollInterval(Duration.ofMillis(500))
				.untilAsserted(() -> {
					var payments = paymentRepository.findByOrderId(orderId, null);
					assertThat(payments.getContent()).isNotEmpty();
					assertThat(payments.getContent().get(0).getPaymentAmount()).isEqualByComparingTo(amount);
					assertThat(payments.getContent().get(0).getStatus()).isEqualTo(Status.SUCCEED);
				});
	}

	@Test
	@DisplayName("Security Flow: Admin can search payments")
	void shouldAllowAdminToSearchPayments() {
		String userId = "some-user";
		NewPaymentDto dto = new NewPaymentDto("ord-1", userId, Status.PENDING, LocalDateTime.now(), BigDecimal.TEN);

		stubFor(get(urlPathMatching("/api/random.*")).willReturn(okJson("[2]")));

		restTemplate.postForEntity("/v1/payments",
				new HttpEntity<>(dto, getAuthHeaders(userId, "USER")), PaymentDto.class);


		String url = "/v1/payments?statuses=SUCCEED";
		HttpEntity<?> request = new HttpEntity<>(getAuthHeaders("admin-user", "ADMIN"));

		ResponseEntity<String> response = restTemplate.exchange(
				url,
				HttpMethod.GET,
				request,
				String.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	@DisplayName("PATCH /v1/payments/{id}: Admin should update status successfully")
	void shouldUpdatePaymentStatus_WhenAdmin() {
		String userId = "user-update-status";
		Payment payment = Payment.builder()
				.orderId("order-to-update")
				.userId(userId)
				.status(Status.PENDING)
				.timestamp(LocalDateTime.now())
				.paymentAmount(BigDecimal.TEN)
				.build();
		payment = paymentRepository.save(payment);

		HttpEntity<?> request = new HttpEntity<>(getAuthHeaders("admin-user", "ADMIN"));

		ResponseEntity<PaymentDto> response = restTemplate.exchange(
				"/v1/payments/" + payment.getId() + "?status=REFUNDED",
				HttpMethod.PATCH,
				request,
				PaymentDto.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().status()).isEqualTo(Status.REFUNDED);

		Payment updatedInDb = paymentRepository.findById(payment.getId()).orElseThrow();
		assertThat(updatedInDb.getStatus()).isEqualTo(Status.REFUNDED);
	}

	@Test
	@DisplayName("GET /v1/payments?userId=...: User should find ONLY their own payments")
	void shouldFindPaymentsByUserId_WhenUserRequestsOwnData() {
		String myUserId = "my-user-id";
		String otherUserId = "other-user-id";


		paymentRepository.save(Payment.builder().orderId("my-ord-1").userId(myUserId).status(Status.SUCCEED).timestamp(LocalDateTime.now()).paymentAmount(BigDecimal.ONE).build());
		paymentRepository.save(Payment.builder().orderId("my-ord-2").userId(myUserId).status(Status.PENDING).timestamp(LocalDateTime.now()).paymentAmount(BigDecimal.TEN).build());

		paymentRepository.save(Payment.builder().orderId("other-ord-1").userId(otherUserId).status(Status.SUCCEED).timestamp(LocalDateTime.now()).paymentAmount(BigDecimal.ZERO).build());

		HttpEntity<?> request = new HttpEntity<>(getAuthHeaders(myUserId, "USER"));

		ResponseEntity<String> response = restTemplate.exchange(
				"/v1/payments?userId=" + myUserId,
				HttpMethod.GET,
				request,
				String.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		String body = response.getBody();
		assertThat(body)
				.contains("my-ord-1")
				.contains("my-ord-2")
				.doesNotContain("other-ord-1");
	}

	@Test
	@DisplayName("GET /v1/payments?userId=...: User cannot search for OTHER users payments")
	void shouldReturnForbidden_WhenUserSearchesOtherUserData() {
		String victimUserId = "victim-user";
		String hackerUserId = "hacker-user";

		HttpEntity<?> request = new HttpEntity<>(getAuthHeaders(hackerUserId, "USER"));

		ResponseEntity<String> response = restTemplate.exchange(
				"/v1/payments?userId=" + victimUserId,
				HttpMethod.GET,
				request,
				String.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	@DisplayName("GET /v1/payments?orderId=...: Admin should find payment by Order ID")
	void shouldFindPaymentByOrderId_WhenAdmin() {
		String orderId = "target-order-123";
		paymentRepository.save(Payment.builder()
				.orderId(orderId)
				.userId("some-user")
				.status(Status.AUTHORIZED)
				.timestamp(LocalDateTime.now())
				.paymentAmount(BigDecimal.valueOf(500))
				.build());

		HttpEntity<?> request = new HttpEntity<>(getAuthHeaders("admin", "ADMIN"));

		ResponseEntity<String> response = restTemplate.exchange(
				"/v1/payments?orderId=" + orderId,
				HttpMethod.GET,
				request,
				String.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains(orderId);
		assertThat(response.getBody()).contains("AUTHORIZED");
	}
}