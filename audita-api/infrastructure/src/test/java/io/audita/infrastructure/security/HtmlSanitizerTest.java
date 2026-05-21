package io.audita.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void sanitize_enforces_link_target_and_rel() {
        String html = "<p>Hello <a href=\"https://audita.io\">Audita</a></p>";

        String sanitized = sanitizer.sanitize(html);

        assertThat(sanitized).contains("target=\"_blank\"");
        assertThat(sanitized).contains("rel=\"noopener noreferrer nofollow\"");
    }

    @Test
    void sanitize_strips_script_content() {
        String html = "<p>Safe</p><script>alert('xss')</script>";

        String sanitized = sanitizer.sanitize(html);

        assertThat(sanitized).contains("Safe");
        assertThat(sanitized).doesNotContain("<script>");
    }
}
