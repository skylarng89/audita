package io.audita.api.config;

import io.audita.api.security.JwtAuthenticationFilter;
import io.audita.api.security.TenantResolutionFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
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
        @SuppressWarnings("java:S4502")
        public SecurityFilterChain filterChain(HttpSecurity http) {
                // This API authenticates normal requests with explicit bearer tokens, not
                // ambient session cookies. The only cookie-backed auth flow is the refresh
                // cookie scoped to /api/v1/auth with SameSite=Strict, which prevents
                // cross-site submission on the state-changing endpoints that consume it.
                http.csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

                http.addFilterAt(new AuthorizationFilter(apiAuthorizationManager()), AuthorizationFilter.class);

                http.addFilterBefore(tenantResolutionFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(jwtFilter, TenantResolutionFilter.class);

                return http.build();
        }

        private RequestMatcherDelegatingAuthorizationManager apiAuthorizationManager() {
                return RequestMatcherDelegatingAuthorizationManager.builder()
						.requestMatchers(methodMatchers(HttpMethod.POST,
								"/api/v1/auth/login",
								"/api/v1/auth/session",
								"/api/v1/auth/refresh",
								"/api/v1/auth/logout",
								"/api/v1/auth/forgot-password",
								"/api/v1/auth/reset-password",
								"/api/v1/auth/accept-invite",
								"/api/platform/v1/bootstrap",
								"/api/platform/v1/setup"))
						.permitAll()
						.requestMatchers(methodMatchers(HttpMethod.GET,
								"/api/platform/v1/bootstrap/status",
								"/api/v1/notifications/stream",
								"/api/v1/audit-trail/exports/download/**",
								"/actuator/health",
								"/actuator/health/**"))
						.permitAll()
                                .requestMatchers(pathMatchers("/api/platform/v1/**"))
                                .hasRole("SUPER_ADMIN")
                                .anyRequest()
                                .authenticated()
                                .build();
        }

        private RequestMatcher[] methodMatchers(HttpMethod method, String... patterns) {
                return Arrays.stream(patterns)
                                .map(pattern -> PathPatternRequestMatcher.pathPattern(method, pattern))
                                .toArray(RequestMatcher[]::new);
        }

        private RequestMatcher[] pathMatchers(String... patterns) {
                return Arrays.stream(patterns)
                                .map(PathPatternRequestMatcher::pathPattern)
                                .toArray(RequestMatcher[]::new);
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
