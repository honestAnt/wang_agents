package com.enterpriseai.admin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.admin"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.admin.repository"})
@EntityScan(basePackages = {"com.enterpriseai.admin.entity"})
@Slf4j
public class AdminModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("AdminModuleAutoConfiguration loaded.");
    }
}
