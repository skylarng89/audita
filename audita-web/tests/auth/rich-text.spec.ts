import { describe, expect, it } from "vitest";
import {
  normalizeLinkHref,
  normalizeRichTextHtml,
} from "~/composables/richText";

describe("rich text helpers", () => {
  it("normalizes link href values", () => {
    expect(normalizeLinkHref("example.com")).toBe("https://example.com");
    expect(normalizeLinkHref("https://audita.io")).toBe("https://audita.io");
    expect(normalizeLinkHref("mailto:test@example.com")).toBe(
      "mailto:test@example.com",
    );
  });

  it("enforces target and rel on anchor tags", () => {
    const html = normalizeRichTextHtml('<p>Hello <a href="https://audita.io">Audita</a></p>');
    expect(html).toContain('target="_blank"');
    expect(html).toContain('rel="noopener noreferrer nofollow"');
  });
});
