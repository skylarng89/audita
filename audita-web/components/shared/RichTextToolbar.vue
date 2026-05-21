<script setup lang="ts">
import type { Editor } from "@tiptap/vue-3";
import { normalizeLinkHref } from "~/composables/richText";

const props = defineProps<{
  editor: Editor | null | undefined;
}>();

function run(command: () => boolean) {
  command();
}

function canRun(command: () => boolean) {
  return props.editor ? command() : false;
}

function isActive(name: string, attributes?: Record<string, unknown>) {
  return props.editor?.isActive(name, attributes) ?? false;
}

function setLink() {
  if (!props.editor) {
    return;
  }

  const previousHref = props.editor.getAttributes("link").href as
    | string
    | undefined;
  const input = globalThis.prompt("Enter URL", previousHref ?? "https://");
  if (input == null) {
    return;
  }

  const normalizedHref = normalizeLinkHref(input);
  if (!normalizedHref) {
    props.editor.chain().focus().unsetLink().run();
    return;
  }

  props.editor
    .chain()
    .focus()
    .extendMarkRange("link")
    .setLink({
      href: normalizedHref,
      rel: "noopener noreferrer nofollow",
      target: "_blank",
    })
    .run();
}
</script>

<template>
  <div class="rich-editor-toolbar" role="toolbar" aria-label="Text formatting">
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('heading', { level: 2 }) }"
      :disabled="
        !canRun(
          () => props.editor?.can().chain().focus().toggleHeading({ level: 2 }).run() ?? false,
        )
      "
      aria-label="Heading"
      @click="run(() => props.editor?.chain().focus().toggleHeading({ level: 2 }).run() ?? false)"
    >
      H2
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('paragraph') }"
      :disabled="
        !canRun(
          () =>
            props.editor?.can().chain().focus().setParagraph().run() ?? false,
        )
      "
      aria-label="Paragraph"
      @click="
        run(() => props.editor?.chain().focus().setParagraph().run() ?? false)
      "
    >
      P
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('bold') }"
      :disabled="
        !canRun(
          () => props.editor?.can().chain().focus().toggleBold().run() ?? false,
        )
      "
      aria-label="Bold"
      @click="
        run(() => props.editor?.chain().focus().toggleBold().run() ?? false)
      "
    >
      B
    </button>
    <button
      type="button"
      class="rich-editor-button italic"
      :class="{ 'rich-editor-button-active': isActive('italic') }"
      :disabled="
        !canRun(
          () =>
            props.editor?.can().chain().focus().toggleItalic().run() ?? false,
        )
      "
      aria-label="Italic"
      @click="
        run(() => props.editor?.chain().focus().toggleItalic().run() ?? false)
      "
    >
      I
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('strike') }"
      :disabled="
        !canRun(
          () =>
            props.editor?.can().chain().focus().toggleStrike().run() ?? false,
        )
      "
      aria-label="Strikethrough"
      @click="
        run(() => props.editor?.chain().focus().toggleStrike().run() ?? false)
      "
    >
      S
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('blockquote') }"
      :disabled="
        !canRun(
          () => props.editor?.can().chain().focus().toggleBlockquote().run() ?? false,
        )
      "
      aria-label="Block quote"
      @click="run(() => props.editor?.chain().focus().toggleBlockquote().run() ?? false)"
    >
      ""
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('bulletList') }"
      :disabled="
        !canRun(
          () =>
            props.editor?.can().chain().focus().toggleBulletList().run() ??
            false,
        )
      "
      aria-label="Bulleted list"
      @click="
        run(
          () => props.editor?.chain().focus().toggleBulletList().run() ?? false,
        )
      "
    >
      UL
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('orderedList') }"
      :disabled="
        !canRun(
          () =>
            props.editor?.can().chain().focus().toggleOrderedList().run() ??
            false,
        )
      "
      aria-label="Numbered list"
      @click="
        run(
          () =>
            props.editor?.chain().focus().toggleOrderedList().run() ?? false,
        )
      "
    >
      OL
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('codeBlock') }"
      :disabled="
        !canRun(
          () => props.editor?.can().chain().focus().toggleCodeBlock().run() ?? false,
        )
      "
      aria-label="Code block"
      @click="run(() => props.editor?.chain().focus().toggleCodeBlock().run() ?? false)"
    >
      Code
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :class="{ 'rich-editor-button-active': isActive('link') }"
      :disabled="
        !canRun(() => props.editor?.can().chain().focus().extendMarkRange('link').run() ?? false)
      "
      aria-label="Insert or edit link"
      @click="setLink"
    >
      Link
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :disabled="!canRun(() => props.editor?.can().chain().focus().unsetLink().run() ?? false)"
      aria-label="Remove link"
      @click="run(() => props.editor?.chain().focus().unsetLink().run() ?? false)"
    >
      Unlink
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :disabled="!canRun(() => props.editor?.can().chain().focus().undo().run() ?? false)"
      aria-label="Undo"
      @click="run(() => props.editor?.chain().focus().undo().run() ?? false)"
    >
      Undo
    </button>
    <button
      type="button"
      class="rich-editor-button"
      :disabled="!canRun(() => props.editor?.can().chain().focus().redo().run() ?? false)"
      aria-label="Redo"
      @click="run(() => props.editor?.chain().focus().redo().run() ?? false)"
    >
      Redo
    </button>
    <button
      type="button"
      class="rich-editor-button rich-editor-button-clear"
      :disabled="
        !canRun(
          () =>
            props.editor
              ?.can()
              .chain()
              .focus()
              .clearNodes()
              .unsetAllMarks()
              .run() ?? false,
        )
      "
      aria-label="Clear formatting"
      @click="
        run(
          () =>
            props.editor?.chain().focus().clearNodes().unsetAllMarks().run() ??
            false,
        )
      "
    >
      Clear
    </button>
  </div>
</template>
