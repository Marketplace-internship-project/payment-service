package io.hohichh.marketplace.payment.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.hohichh.marketplace.payment.integration.config.TestAppConfig;
import io.hohichh.marketplace.payment.integration.config.TestClockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({
        TestClockConfiguration.class,
        TestAppConfig.class
})
public abstract class AbstractApplicationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected Clock clock;

    @Value("${jwt.access.secret}")
    private String jwtSecret;

    protected static WireMockServer wireMockServer;

    private static final Instant FIXED_TIME = Instant.parse("2025-01-01T12:00:00Z");

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("payment.external-bank-api-url",
                () -> "http://localhost:" + wireMockServer.port() + "/api/random");
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();

        when(clock.instant()).thenReturn(FIXED_TIME);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    }

    protected HttpHeaders getAuthHeaders(String userId, String role) {
        String token = generateTestToken(userId, role);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String generateTestToken(String userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

         Date issuedAt = Date.from(clock.instant());
        Date expiration = Date.from(clock.instant().plus(1, ChronoUnit.HOURS));

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    static class TestPage<T> {
        public List<T> content;
        public long totalElements;
        public int totalPages;
        public int size;
        public int number;

        public List<T> getContent() { return content; }
        public long getTotalElements() { return totalElements; }
    }
}