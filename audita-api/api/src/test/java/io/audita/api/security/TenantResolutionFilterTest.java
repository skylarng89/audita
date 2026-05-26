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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantResolutionFilterTest {

    @Mock
    DataSource dataSource;
    @Mock
    FilterChain filterChain;

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

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> tenantResolutionFilter.doFilter(request, response, filterChain));

        org.assertj.core.api.Assertions.assertThat(exception)
                .hasMessage("Tenant header is not allowed for bootstrap requests.");

        verifyNoInteractions(dataSource);
        verifyNoInteractions(filterChain);
    }

    @Test
    void invalid_tenant_slug_is_rejected() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/change-requests");
        request.addHeader("X-Tenant-Slug", "tenant';drop");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> tenantResolutionFilter.doFilter(request, response, filterChain));

        org.assertj.core.api.Assertions.assertThat(exception)
                .hasMessage("Invalid tenant slug.");

        verifyNoInteractions(dataSource);
        verifyNoInteractions(filterChain);
    }

    @Test
    void tenant_header_matching_subdomain_resolves_to_slug() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tenantExistsStatement = mock(PreparedStatement.class);
        PreparedStatement subdomainStatement = mock(PreparedStatement.class);
        ResultSet tenantExistsResult = mock(ResultSet.class);
        ResultSet subdomainResult = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT 1 FROM public.tenants WHERE slug = ?")).thenReturn(tenantExistsStatement);
        when(connection.prepareStatement("SELECT slug FROM public.tenants WHERE subdomain = ?")).thenReturn(subdomainStatement);
        when(tenantExistsStatement.executeQuery()).thenReturn(tenantExistsResult);
        when(subdomainStatement.executeQuery()).thenReturn(subdomainResult);
        when(tenantExistsResult.next()).thenReturn(false);
        when(subdomainResult.next()).thenReturn(true);
        when(subdomainResult.getString(1)).thenReturn("pixelpay-systems-limited");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/accept-invite");
        request.addHeader("X-Tenant-Slug", "cm");
        MockHttpServletResponse response = new MockHttpServletResponse();

        tenantResolutionFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
