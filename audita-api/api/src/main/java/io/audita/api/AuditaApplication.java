package io.audita.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "io.audita")
@EnableAsync
@EnableScheduling
public class AuditaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuditaApplication.class, args);
    }
}
