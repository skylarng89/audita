package io.audita.api.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TenantResolutionFilterTest {

    @Mock DataSource dataSource;
    @Mock FilterChain filterChain;

    private TenantResolutionFilter tenantResolutionFilter;

    @BeforeEach
    void setUp() {
        tenantResolutionFilter = new TenantResolutionFilter(dataSource);
    }

    @Test
    void bootstrap_request_with_tenant_header_is_rejected() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/platform/v1/bootstrap");
        request.addHeader("X-Tenant-Slug", "tenant-acme");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> tenantResolutionFilter.doFilter(request, response, filterChain))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Tenant header is not allowed for bootstrap requests.");

        verifyNoInteractions(dataSource);
        verifyNoInteractions(filterChain);
    }

    @Test
    void invalid_tenant_slug_is_rejected() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/change-requests");
        request.addHeader("X-Tenant-Slug", "tenant';drop");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> tenantResolutionFilter.doFilter(request, response, filterChain))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Invalid tenant slug.");

        verifyNoInteractions(dataSource);
        verifyNoInteractions(filterChain);
    }
}
