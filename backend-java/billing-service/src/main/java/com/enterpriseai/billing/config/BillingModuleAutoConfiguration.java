package com.enterpriseai.billing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.billing"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.billing.repository"})
@EntityScan(basePackages = {"com.enterpriseai.billing.entity"})
@Slf4j
public class BillingModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("BillingModuleAutoConfiguration loaded.");
    }
}
