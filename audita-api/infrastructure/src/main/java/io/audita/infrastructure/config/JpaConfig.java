package io.audita.infrastructure.config;

import io.audita.infrastructure.tenant.AuditaMultiTenantConnectionProvider;
import io.audita.infrastructure.tenant.AuditaTenantIdentifierResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class JpaConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            AuditaMultiTenantConnectionProvider connectionProvider,
            AuditaTenantIdentifierResolver tenantResolver) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("io.audita.infrastructure.persistence.entity");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaPropertyMap(hibernateProperties(connectionProvider, tenantResolver));

        return em;
    }

    private Map<String, Object> hibernateProperties(
            AuditaMultiTenantConnectionProvider connectionProvider,
            AuditaTenantIdentifierResolver tenantResolver) {

        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.multiTenancy", "SCHEMA");
        props.put("hibernate.multi_tenant_connection_provider", connectionProvider);
        props.put("hibernate.tenant_identifier_resolver", tenantResolver);
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.format_sql", "false");
        props.put("hibernate.show_sql", "false");
        // DDL is managed by Flyway — never by Hibernate
        props.put("hibernate.hbm2ddl.auto", "none");
        return props;
    }
}
