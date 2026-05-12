package io.audita.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractHeaderFilterTest {

    @Test
    void adds_api_contract_header_to_response() throws Exception {
        ApiContractHeaderFilter filter = new ApiContractHeaderFilter("2026-05-12-auth-v2");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(ApiContractHeaderFilter.API_CONTRACT_HEADER))
                .isEqualTo("2026-05-12-auth-v2");
    }
}