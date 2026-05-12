package io.audita.api.config;

import io.audita.api.security.JwtAuthenticationFilter;
import io.audita.api.security.TenantResolutionFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.config.Customizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private static final String REQUEST_MATCHERS_METHOD = "requestMatchers";

        private final JwtAuthenticationFilter jwtFilter;
        private final TenantResolutionFilter tenantResolutionFilter;
        private final String corsAllowedOrigins;

        public SecurityConfig(
                        JwtAuthenticationFilter jwtFilter,
                        TenantResolutionFilter tenantResolutionFilter,
                        @Value("${audita.cors.allowed-origins}") String corsAllowedOrigins) {
                this.jwtFilter = jwtFilter;
                this.tenantResolutionFilter = tenantResolutionFilter;
                this.corsAllowedOrigins = corsAllowedOrigins;
        }

        @Bean
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public SecurityFilterChain filterChain(HttpSecurity http) {
                http.csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authorizeRequests());

                http.addFilterBefore(tenantResolutionFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(jwtFilter, TenantResolutionFilter.class);

                return http.build();
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private Customizer authorizeRequests() {
                return registry -> {
                        permitAll(registry, HttpMethod.POST,
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/session",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/logout",
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/reset-password",
                                        "/api/v1/auth/accept-invite",
                                        "/api/v1/auth/oauth/exchange",
                                        "/api/platform/v1/bootstrap",
                                        "/api/platform/v1/setup");
                        permitAll(registry, HttpMethod.GET,
                                        "/api/platform/v1/bootstrap/status");
                        permitAll(registry, HttpMethod.GET,
                                        "/api/v1/auth/oauth/**");
                        permitAll(registry, HttpMethod.GET,
                                        "/api/v1/notifications/stream");
                        permitAll(registry, "/actuator/health");
                        hasRole(registry, "SUPER_ADMIN", "/api/platform/v1/**");
                        authenticated(registry);
                };
        }

        private void permitAll(Object registry, HttpMethod method, String... patterns) {
                Object matcher = invoke(registry, REQUEST_MATCHERS_METHOD,
                                new Class<?>[] { HttpMethod.class, String[].class },
                                method, patterns);
                invoke(matcher, "permitAll", new Class<?>[0]);
        }

        private void permitAll(Object registry, String... patterns) {
                Object matcher = invoke(registry, REQUEST_MATCHERS_METHOD, new Class<?>[] { String[].class },
                                (Object) patterns);
                invoke(matcher, "permitAll", new Class<?>[0]);
        }

        private void hasRole(Object registry, String role, String... patterns) {
                Object matcher = invoke(registry, REQUEST_MATCHERS_METHOD, new Class<?>[] { String[].class },
                                (Object) patterns);
                invoke(matcher, "hasRole", new Class<?>[] { String.class }, role);
        }

        private void authenticated(Object registry) {
                Object matcher = invoke(registry, "anyRequest", new Class<?>[0]);
                invoke(matcher, "authenticated", new Class<?>[0]);
        }

        private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
                try {
                        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
                } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException("Failed to configure Spring Security authorization rules.",
                                        exception);
                }
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                // bcrypt cost factor 12 (AUTH-01)
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
                if (allowedOrigins.isEmpty()) {
                        throw new IllegalStateException("audita.cors.allowed-origins must be explicitly configured.");
                }
                if (allowedOrigins.stream().anyMatch(origin -> origin.equals("*") || origin.contains("*"))) {
                        throw new IllegalStateException(
                                        "Wildcard CORS origins are not allowed when credentials are enabled.");
                }
                config.setAllowedOrigins(allowedOrigins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-Slug", "X-Setup-Token"));
                config.setExposedHeaders(List.of("X-Audita-Api-Contract"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
