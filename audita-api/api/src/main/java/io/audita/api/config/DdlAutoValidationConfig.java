package io.audita.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DdlAutoValidationConfig {

    private static final Logger log = LoggerFactory.getLogger(DdlAutoValidationConfig.class);

    private final Environment environment;

    @Value("${spring.jpa.hibernate.ddl-auto:}")
    private String ddlAuto;

    public DdlAutoValidationConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateDdlAuto() {
        if (environment.matchesProfiles("dev")) {
            return;
        }

        String value = ddlAuto;
        if (value == null || value.isEmpty()) {
            value = "validate";
        }

        if ("update".equalsIgnoreCase(value) || "create".equalsIgnoreCase(value) || "create-drop".equalsIgnoreCase(value)) {
            log.error(
                    "FATAL: spring.jpa.hibernate.ddl-auto='{}' is unsafe for non-dev profiles. "
                    + "Set JPA_DDL_AUTO=validate in production.",
                    value
            );
            throw new IllegalStateException(
                    "Unsafe ddl-auto value '" + value + "' in non-dev profile. Must be 'validate'."
            );
        }
    }
}
