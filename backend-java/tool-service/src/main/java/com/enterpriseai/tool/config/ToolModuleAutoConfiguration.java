package com.enterpriseai.tool.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.tool"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.tool.repository"})
@EntityScan(basePackages = {"com.enterpriseai.tool.entity"})
@Slf4j
public class ToolModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("ToolModuleAutoConfiguration loaded.");
    }
}
