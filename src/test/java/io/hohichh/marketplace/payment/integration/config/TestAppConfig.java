package io.hohichh.marketplace.payment.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestAppConfig {

    @Bean
    @ServiceConnection
    public MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
    }

    @Bean
    @ServiceConnection
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));
    }
}