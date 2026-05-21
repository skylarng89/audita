import Link from "@tiptap/extension-link";
import Placeholder from "@tiptap/extension-placeholder";
import StarterKit from "@tiptap/starter-kit";

const LINK_REL = "noopener noreferrer nofollow";
const LINK_TARGET = "_blank";

export function buildRichTextExtensions(placeholder: string) {
  return [
    StarterKit,
    Link.configure({
      HTMLAttributes: {
        rel: LINK_REL,
        target: LINK_TARGET,
      },
      openOnClick: false,
      protocols: ["http", "https", "mailto", "tel"],
    }),
    Placeholder.configure({ placeholder }),
  ];
}

export function normalizeLinkHref(rawUrl: string): string {
  const value = rawUrl.trim();
  if (!value) {
    return "";
  }
  if (/^[a-zA-Z][a-zA-Z\d+.-]*:/.test(value)) {
    return value;
  }
  if (value.startsWith("//")) {
    return `https:${value}`;
  }
  return `https://${value}`;
}

export function normalizeRichTextHtml(html: string | null | undefined): string {
  if (!html) {
    return "";
  }
  if (typeof DOMParser === "undefined") {
    return html.replace(/<a\b([^>]*)>/gi, (full, attrs: string) => {
      const hrefMatch = attrs.match(/href\s*=\s*(["'])(.*?)\1/i);
      if (!hrefMatch) {
        return "<a>";
      }
      return `<a href="${hrefMatch[2]}" target="_blank" rel="${LINK_REL}">`;
    });
  }

  const parser = new DOMParser();
  const document = parser.parseFromString(html, "text/html");
  document.querySelectorAll("a[href]").forEach((anchor) => {
    anchor.setAttribute("target", LINK_TARGET);
    anchor.setAttribute("rel", LINK_REL);
  });
  return document.body.innerHTML;
}
