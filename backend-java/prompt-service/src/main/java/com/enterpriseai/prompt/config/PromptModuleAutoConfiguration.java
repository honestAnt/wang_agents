package com.enterpriseai.prompt.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.prompt"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.prompt.repository"})
@EntityScan(basePackages = {"com.enterpriseai.prompt.entity"})
@Slf4j
public class PromptModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("PromptModuleAutoConfiguration loaded.");
    }
}
