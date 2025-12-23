package io.hohichh.marketplace.payment;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;

@SpringBootApplication
public class Application {
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
					return builder
				.connectTimeout(java.time.Duration.ofSeconds(3))
				.readTimeout(java.time.Duration.ofSeconds(3))
				.build();
	}
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
