package io.audita.api.config;

import io.audita.api.security.JwtAuthenticationFilter;
import io.audita.api.security.TenantResolutionFilter;
import java.util.Arrays;
import java.util.List;
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
        public SecurityFilterChain filterChain(HttpSecurity http) {
                return http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Public auth endpoints
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/refresh",
                                                                "/api/v1/auth/logout",
                                                                "/api/v1/auth/forgot-password",
                                                                "/api/v1/auth/reset-password",
                                                                "/api/v1/auth/accept-invite",
                                                                "/api/v1/auth/oauth/exchange",
                                                                "/api/platform/v1/bootstrap",
                                                                "/api/platform/v1/setup")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/platform/v1/bootstrap/status")
                                                .permitAll()
                                                // SSO redirect initiation (GET — browser navigates here)
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/v1/auth/oauth/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/v1/notifications/stream")
                                                .permitAll()
                                                // Actuator health (for container probes)
                                                .requestMatchers("/actuator/health").permitAll()
                                                // Platform (Super Admin) routes
                                                .requestMatchers("/api/platform/v1/**").hasRole("SUPER_ADMIN")
                                                // Everything else requires authentication
                                                .anyRequest().authenticated())
                                .addFilterBefore(tenantResolutionFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(jwtFilter, TenantResolutionFilter.class)
                                .build();
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
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
