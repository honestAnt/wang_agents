package com.enterpriseai.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.common"})
@EnableFeignClients(basePackages = {"com.enterpriseai.common.feign"})
@Slf4j
public class CommonLibAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("CommonLibAutoConfiguration loaded.");
    }
}
