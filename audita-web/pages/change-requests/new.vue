<template>
  <div class="max-w-5xl mx-auto space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs text-primary/70 uppercase tracking-[0.16em] font-semibold mb-1"
        >
          Change Requests
        </p>
        <h1
          class="text-4xl font-bold tracking-tight text-gray-900 dark:text-gray-100"
        >
          Create Change Request
        </h1>
      </div>
      <button class="btn-ghost btn-md" @click="navigateTo('/change-requests')">
        Back
      </button>
    </div>

    <form
      class="card p-8 space-y-7 shadow-card-hover"
      @submit.prevent="createChangeRequest"
    >
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="md:col-span-2">
          <label class="field-label">Change Title</label>
          <input
            v-model="form.title"
            class="input mt-1"
            placeholder="e.g., Kubernetes Cluster Migration - Q3"
            maxlength="500"
            required
          />
        </div>

        <div>
          <label class="field-label">Priority Level</label>
          <select v-model="form.priority" class="input mt-1" required>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>

        <div>
          <label class="field-label">Risk Assessment</label>
          <select v-model="form.riskLevel" class="input mt-1" required>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>

        <div>
          <label class="field-label">Approval Type</label>
          <select v-model="form.approvalType" class="input mt-1" required>
            <option value="LINEAR">Linear</option>
            <option value="NON_LINEAR">Non Linear</option>
          </select>
        </div>

        <div>
          <label class="field-label">Category</label>
          <input
            v-model="form.category"
            class="input mt-1"
            maxlength="255"
            placeholder="Infrastructure / Application / Security"
          />
        </div>

        <div>
          <label class="field-label">Scheduled Start</label>
          <ClientOnly>
            <VueDatePicker
              v-model="form.scheduledStart"
              class="mt-1"
              :enable-time-picker="true"
              :is24="false"
              time-picker-inline
              placeholder="Select date and time"
              :min-date="new Date()"
              format="MMM d, yyyy h:mm aa"
              auto-apply
            />
          </ClientOnly>
        </div>

        <div>
          <label class="field-label">Scheduled End</label>
          <ClientOnly>
            <VueDatePicker
              v-model="form.scheduledEnd"
              class="mt-1"
              :enable-time-picker="true"
              :is24="false"
              time-picker-inline
              placeholder="Select date and time"
              :min-date="form.scheduledStart ?? new Date()"
              format="MMM d, yyyy h:mm aa"
              auto-apply
            />
          </ClientOnly>
        </div>

        <div class="md:col-span-2">
          <label class="field-label">Affected Systems (comma separated)</label>
          <input
            v-model="form.affectedSystemsInput"
            class="input mt-1"
            placeholder="payment-api, nginx-prod, postgres-replica"
          />
        </div>

        <div class="md:col-span-2">
          <label class="field-label">Scope and Description</label>
          <ClientOnly>
            <EditorContent :editor="editor" class="input mt-1 min-h-44 p-4" />
          </ClientOnly>
          <p class="field-hint">
            Define technical scope, constraints, and rollback strategy.
          </p>
        </div>
      </div>

      <div class="flex justify-end gap-2">
        <button
          type="button"
          class="btn-ghost btn-md"
          @click="navigateTo('/change-requests')"
        >
          Cancel
        </button>
        <button class="btn-primary btn-md" :disabled="isSaving">
          {{ isSaving ? "Creating…" : "Create Draft" }}
        </button>
      </div>
      <p v-if="errorMessage" class="text-sm text-red-600">{{ errorMessage }}</p>
    </form>
  </div>
</template>

<script setup lang="ts">
import { EditorContent, useEditor } from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";

definePageMeta({ middleware: "auth" });

const { create } = useChangeRequests();

const isSaving = ref(false);
const errorMessage = ref("");
const editor = useEditor({
  content: "<p></p>",
  extensions: [StarterKit],
  editorProps: {
    attributes: {
      class: "prose dark:prose-invert max-w-none focus:outline-none",
    },
  },
});

const form = reactive({
  title: "",
  priority: "MEDIUM",
  riskLevel: "MEDIUM",
  approvalType: "LINEAR",
  category: "",
  scheduledStart: null as Date | null,
  scheduledEnd: null as Date | null,
  affectedSystemsInput: "",
});

async function createChangeRequest() {
  errorMessage.value = "";
  isSaving.value = true;
  try {
    const payload = {
      title: form.title,
      description: editor.value?.getHTML() || null,
      priority: form.priority,
      riskLevel: form.riskLevel,
      approvalType: form.approvalType,
      category: form.category || null,
      scheduledStart: form.scheduledStart
        ? form.scheduledStart.toISOString()
        : null,
      scheduledEnd: form.scheduledEnd ? form.scheduledEnd.toISOString() : null,
      affectedSystems: form.affectedSystemsInput
        .split(",")
        .map((v) => v.trim())
        .filter(Boolean),
    };
    const created = await create(payload);
    await navigateTo(`/change-requests/${created.id}`);
  } catch (error: any) {
    errorMessage.value =
      error?.data?.detail || "Unable to create change request.";
  } finally {
    isSaving.value = false;
  }
}

onBeforeUnmount(() => {
  editor.value?.destroy();
});
</script>
