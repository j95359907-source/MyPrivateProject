package com.quantfund.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.quantfund")
@EntityScan(basePackages = "com.quantfund.common.entity")
@EnableJpaRepositories(basePackages = "com.quantfund.common.repository")
@EnableScheduling
@EnableCaching
public class QuantFundApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantFundApplication.class, args);
    }
}
