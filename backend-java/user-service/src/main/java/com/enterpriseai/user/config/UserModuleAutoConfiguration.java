package com.enterpriseai.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.user"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.user.repository"})
@EntityScan(basePackages = {"com.enterpriseai.user.entity"})
@Slf4j
public class UserModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("UserModuleAutoConfiguration loaded.");
    }
}
