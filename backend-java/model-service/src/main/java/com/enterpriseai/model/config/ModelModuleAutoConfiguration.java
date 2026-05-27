package com.enterpriseai.model.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.model"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.model.repository"})
@EntityScan(basePackages = {"com.enterpriseai.model.entity"})
@Slf4j
public class ModelModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("ModelModuleAutoConfiguration loaded.");
    }
}
