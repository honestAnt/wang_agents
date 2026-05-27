package com.enterpriseai.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.auth"})
@Slf4j
public class AuthModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("AuthModuleAutoConfiguration loaded.");
    }
}
