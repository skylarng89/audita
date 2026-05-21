package io.audita.infrastructure.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side HTML sanitiser for TipTap rich-text output.
 * Applied before any rich-text content (CR descriptions, comments) is persisted.
 *
 * Policy: allow formatting, links, and lists. Deny scripts, iframes, event handlers.
 */
@Component
public class HtmlSanitizer {

    private static final Pattern ANCHOR_OPENING_TAG = Pattern.compile("<a\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HREF_ATTRIBUTE = Pattern.compile("href\\s*=\\s*([\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE);
    private static final PolicyFactory LINK_POLICY = new HtmlPolicyBuilder()
            .allowElements("a")
            .allowAttributes("href", "target", "rel").onElements("a")
            .requireRelNofollowOnLinks()
            .toFactory();

    private static final PolicyFactory POLICY =
            Sanitizers.FORMATTING
            .and(LINK_POLICY)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.TABLES);

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        String normalizedLinks = normalizeAnchorAttributes(html);
        return POLICY.sanitize(normalizedLinks);
    }

    private String normalizeAnchorAttributes(String html) {
        Matcher matcher = ANCHOR_OPENING_TAG.matcher(html);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String attributes = matcher.group(1);
            Matcher hrefMatcher = HREF_ATTRIBUTE.matcher(attributes);
            if (!hrefMatcher.find()) {
                matcher.appendReplacement(output, Matcher.quoteReplacement("<a>"));
                continue;
            }
            String href = hrefMatcher.group(2).trim().replace("\"", "&quot;");
            String replacement = "<a href=\"" + href + "\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">";
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
