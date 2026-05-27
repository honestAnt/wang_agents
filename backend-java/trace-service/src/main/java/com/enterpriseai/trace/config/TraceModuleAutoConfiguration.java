package com.enterpriseai.trace.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.trace"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.trace.repository"})
@EntityScan(basePackages = {"com.enterpriseai.trace.entity"})
@EnableKafka
@EnableScheduling
@Slf4j
public class TraceModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("TraceModuleAutoConfiguration loaded.");
    }
}
