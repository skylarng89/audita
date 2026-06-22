import Mention from "@tiptap/extension-mention";
import tippy from "tippy.js";
import type { SuggestionProps, SuggestionKeyDownProps } from "@tiptap/suggestion";
import type { Instance as TippyInstance } from "tippy.js";
import { useApi } from "~/composables/useApi";

interface MentionUser {
  id: string;
  label: string;
  email: string;
}

interface MentionSuggestionItem {
  id: string;
  label: string;
  email: string;
}

interface MentionPopupController {
  update: (props: SuggestionProps<MentionSuggestionItem>) => void;
  onKeyDown: (props: SuggestionKeyDownProps) => boolean;
  destroy: () => void;
}

export async function searchMentionUsers(query: string): Promise<MentionUser[]> {
  const api = useApi();
  try {
    const results = await api<Array<{ id: string; fullName: string; email: string }>>(
      "/api/v1/users/mention-candidates",
      { query: { query, limit: 10 } },
    );
    return results.map((u) => ({
      id: u.id,
      label: u.fullName,
      email: u.email,
    }));
  } catch {
    return [];
  }
}

export function createMentionPopup(props: SuggestionProps<MentionSuggestionItem>): MentionPopupController {
  const container = document.createElement("div");
  container.style.cssText =
    "background:#fff;border:1px solid #e5e7eb;border-radius:8px;box-shadow:0 4px 12px rgba(0,0,0,0.15);padding:4px;max-height:240px;overflow-y:auto;width:320px;";

  let currentCommand = props.command;
  let currentItems: MentionSuggestionItem[] = props.items || [];
  let selectedIndex = 0;

  function renderItems(items: MentionSuggestionItem[], idx: number) {
    currentItems = items;
    selectedIndex = idx;
    container.innerHTML = "";
    if (!items.length) {
      const empty = document.createElement("div");
      empty.style.cssText = "padding:8px 12px;color:#6b7280;font-size:12px;";
      empty.textContent = "Searching…";
      container.appendChild(empty);
      return;
    }
    items.forEach((item, i) => {
      const row = document.createElement("div");
      row.setAttribute("data-index", String(i));
      row.style.cssText =
        "display:flex;flex-direction:column;padding:6px 12px;border-radius:4px;cursor:pointer;" +
        (i === idx
          ? "background:#e8edf5;"
          : "color:#111827;");
      row.addEventListener("mouseenter", () => {
        if (selectedIndex !== i) {
          renderItems(currentItems, i);
        }
      });
      const nameSpan = document.createElement("span");
      nameSpan.style.cssText =
        "font-weight:500;font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;" +
        (i === idx ? "color:#1d3a8a;" : "color:#111827;");
      nameSpan.textContent = item.label;
      const emailSpan = document.createElement("span");
      emailSpan.style.cssText =
        "font-size:12px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;" +
        (i === idx ? "color:#4a6fa5;" : "color:#6b7280;");
      emailSpan.textContent = item.email;
      row.appendChild(nameSpan);
      row.appendChild(emailSpan);
      row.addEventListener("mousedown", (e) => {
        e.preventDefault();
        currentCommand(item);
      });
      container.appendChild(row);
    });
  }

  const instances = tippy("body", {
    getReferenceClientRect: () =>
      (props.clientRect?.() as DOMRect) || new DOMRect(0, 0, 0, 0),
    appendTo: () => document.body,
    content: container,
    showOnCreate: true,
    interactive: true,
    trigger: "manual",
    placement: "bottom-start",
    maxWidth: "320px",
  });
  const tip: TippyInstance | undefined = instances[0];
  if (!tip) {
    return { update: () => {}, onKeyDown: () => false, destroy: () => {} };
  }

  renderItems(props.items || [], 0);

  return {
    update: (updatedProps: SuggestionProps<MentionSuggestionItem>) => {
      currentCommand = updatedProps.command;
      tip.setProps({
        getReferenceClientRect: () =>
          (updatedProps.clientRect?.() as DOMRect) || new DOMRect(0, 0, 0, 0),
      });
      renderItems(updatedProps.items || [], selectedIndex);
    },
    onKeyDown: (updatedProps: SuggestionKeyDownProps) => {
      if (!currentItems.length) return false;
      if (updatedProps.event.key === "ArrowUp") {
        updatedProps.event.preventDefault();
        const idx = (selectedIndex + currentItems.length - 1) % currentItems.length;
        renderItems(currentItems, idx);
        return true;
      }
      if (updatedProps.event.key === "ArrowDown" || updatedProps.event.key === "Tab") {
        updatedProps.event.preventDefault();
        const idx = (selectedIndex + 1) % currentItems.length;
        renderItems(currentItems, idx);
        return true;
      }
      if (updatedProps.event.key === "Enter") {
        updatedProps.event.preventDefault();
        if (selectedIndex >= 0 && selectedIndex < currentItems.length) {
          currentCommand(currentItems[selectedIndex]);
        }
        return true;
      }
      return false;
    },
    destroy: () => {
      tip.destroy();
    },
  };
}

export function buildMentionExtension() {
  return Mention.configure({
    HTMLAttributes: { class: "mention" },
    suggestion: {
      char: "@",
      items: async ({ query }: { query: string }) => {
        const users = await searchMentionUsers(query);
        return users.map((u) => ({
          id: u.id,
          label: u.label,
          email: u.email,
        }));
      },
      render: () => {
        let popup: MentionPopupController | null = null;
        return {
          onStart: (props: SuggestionProps<MentionSuggestionItem>) => {
            popup = createMentionPopup(props);
          },
          onUpdate: (props: SuggestionProps<MentionSuggestionItem>) => {
            popup?.update(props);
          },
          onKeyDown: (props: SuggestionKeyDownProps) => {
            return popup?.onKeyDown(props) ?? false;
          },
          onExit: () => {
            popup?.destroy();
          },
        };
      },
    },
  });
}
