package io.audita.api;

import io.audita.api.config.AuditaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "io.audita")
@EnableConfigurationProperties(AuditaProperties.class)
@EnableAsync
@EnableScheduling
@EnableCaching
public class AuditaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditaApplication.class, args);
    }
}
