package com.enterpriseai.audit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.audit"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.audit.repository"})
@EntityScan(basePackages = {"com.enterpriseai.audit.entity"})
@Slf4j
public class AuditModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("AuditModuleAutoConfiguration loaded.");
    }
}
