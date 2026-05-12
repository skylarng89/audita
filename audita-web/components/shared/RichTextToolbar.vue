<script setup lang="ts">
import type { Editor } from "@tiptap/vue-3";

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
</script>

<template>
  <div class="rich-editor-toolbar" role="toolbar" aria-label="Text formatting">
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
