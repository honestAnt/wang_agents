package com.enterpriseai.skill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.skill"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.skill.repository"})
@EntityScan(basePackages = {"com.enterpriseai.skill.entity"})
@Slf4j
public class SkillModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("SkillModuleAutoConfiguration loaded.");
    }
}
