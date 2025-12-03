package io.hohichh.marketplace.payment.integration.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Configuration;


//TODO: TEMPORALLY
@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class,
                                    HibernateJpaAutoConfiguration.class })
public class TestAppConfig {
}
