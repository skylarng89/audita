package io.audita.api.config;

import io.audita.api.security.JwtAuthenticationFilter;
import io.audita.api.security.TenantResolutionFilter;
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

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final TenantResolutionFilter tenantResolutionFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          TenantResolutionFilter tenantResolutionFilter) {
        this.jwtFilter = jwtFilter;
        this.tenantResolutionFilter = tenantResolutionFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/accept-invite",
                                "/api/platform/v1/bootstrap"
                        ).permitAll()
                        // SSO redirect initiation (GET — browser navigates here)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/auth/oauth/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/notifications/stream"
                        ).permitAll()
                        // Actuator health (for container probes)
                        .requestMatchers("/actuator/health").permitAll()
                        // Platform (Super Admin) routes
                        .requestMatchers("/api/platform/v1/**").hasRole("SUPER_ADMIN")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
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
        // Populated from environment — see CorsProperties
        config.setAllowedOriginPatterns(List.of("*")); // Tightened in production via env var
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
