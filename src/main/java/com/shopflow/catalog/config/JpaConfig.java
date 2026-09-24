package com.shopflow.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;


import java.time.Clock;

@Configuration
@EnableJpaAuditing
@EnableRetry
@EnableScheduling
public class JpaConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
