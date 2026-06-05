<template>
  <section class="card p-5 space-y-4">
    <h2 class="font-semibold">Comments</h2>
    <div class="space-y-3">
      <div
        v-for="comment in comments"
        :key="comment.id"
        :id="`comment-${comment.id}`"
        class="border border-border dark:border-border-dark rounded-lg p-3"
        :class="{ 'ring-2 ring-primary/50 bg-primary/5': highlightedId === comment.id }"
      >
        <p class="text-xs text-muted">
          {{ comment.author?.fullName ?? "Unknown" }} •
          {{ fmt(comment.createdAt) }}
        </p>
        <div
          class="text-sm mt-2 text-gray-800 dark:text-gray-200 rich-content"
          v-html="normalizeRichTextHtml(comment.body)"
        />
      </div>
      <div v-if="!comments.length" class="text-sm text-muted">
        No comments yet.
      </div>
    </div>

    <div class="space-y-2">
      <div class="border border-border dark:border-border-dark rounded-lg overflow-hidden">
        <SharedRichTextToolbar :editor="editor" />
        <EditorContent
          :editor="editor"
          class="rich-editor-content min-h-[120px] p-3"
          placeholder="Add a comment. Type @ to mention someone…"
        />
      </div>
      <div class="flex justify-end">
        <button class="btn-primary btn-md" @click="$emit('post')">
          Post Comment
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { Comment } from "~/types"
import type { Editor } from "@tiptap/vue-3"
import { EditorContent } from "@tiptap/vue-3"
import { normalizeRichTextHtml } from "~/composables/richText"

defineProps<{
  comments: Comment[]
  highlightedId: string | null
  editor: Editor | undefined
  fmt: (value: string) => string
}>()

defineEmits<{
  post: []
}>()
</script>
