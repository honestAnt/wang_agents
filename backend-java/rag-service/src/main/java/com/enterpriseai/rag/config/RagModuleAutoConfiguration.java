package com.enterpriseai.rag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.rag"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.rag.repository"})
@EntityScan(basePackages = {"com.enterpriseai.rag.entity"})
@Slf4j
public class RagModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("RagModuleAutoConfiguration loaded.");
    }
}
