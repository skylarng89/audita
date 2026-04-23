package io.audita.infrastructure.security;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Server-side HTML sanitiser for TipTap rich-text output.
 * Applied before any rich-text content (CR descriptions, comments) is persisted.
 *
 * Policy: allow formatting, links, and lists. Deny scripts, iframes, event handlers.
 */
@Component
public class HtmlSanitizer {

    private static final PolicyFactory POLICY =
            Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.TABLES);

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return POLICY.sanitize(html);
    }
}
