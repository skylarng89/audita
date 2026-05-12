package io.audita.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiContractHeaderFilter extends OncePerRequestFilter {

    public static final String API_CONTRACT_HEADER = "X-Audita-Api-Contract";

    private final String apiContractVersion;

    public ApiContractHeaderFilter(
            @Value("${audita.api.contract-version:2026-05-12-auth-v2}") String apiContractVersion) {
        this.apiContractVersion = apiContractVersion;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(API_CONTRACT_HEADER, apiContractVersion);
        filterChain.doFilter(request, response);
    }
}