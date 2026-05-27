package com.enterpriseai.memory.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.memory"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.memory.repository"})
@EntityScan(basePackages = {"com.enterpriseai.memory.entity"})
@Slf4j
public class MemoryModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("MemoryModuleAutoConfiguration loaded.");
    }
}
