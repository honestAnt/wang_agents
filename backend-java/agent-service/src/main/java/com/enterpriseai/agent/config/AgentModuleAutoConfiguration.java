package com.enterpriseai.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.agent"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.agent.repository"})
@EntityScan(basePackages = {"com.enterpriseai.agent.entity"})
@Slf4j
public class AgentModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("AgentModuleAutoConfiguration loaded.");
    }
}
