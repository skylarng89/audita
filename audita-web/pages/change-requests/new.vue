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
      novalidate
    >
      <!-- Required field legend -->
      <p class="text-xs text-muted">
        Fields marked <span class="text-danger font-semibold">*</span> are
        required.
      </p>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <!-- Title -->
        <div class="md:col-span-2">
          <label class="field-label"
            >Change Title <span class="text-danger">*</span></label
          >
          <input
            v-model="form.title"
            class="input mt-1"
            :class="{ 'input-error': errors.title }"
            placeholder="e.g., Kubernetes Cluster Migration - Q3"
            maxlength="500"
            @blur="touch('title')"
          />
          <p v-if="errors.title" class="field-error">{{ errors.title }}</p>
        </div>

        <!-- Priority -->
        <div>
          <label class="field-label"
            >Priority Level <span class="text-danger">*</span></label
          >
          <select
            v-model="form.priority"
            class="input mt-1"
            :class="{ 'input-error': errors.priority }"
            @change="touch('priority')"
          >
            <option value="" disabled>Select priority</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
          <p v-if="errors.priority" class="field-error">
            {{ errors.priority }}
          </p>
        </div>

        <!-- Risk -->
        <div>
          <label class="field-label"
            >Risk Assessment <span class="text-danger">*</span></label
          >
          <select
            v-model="form.riskLevel"
            class="input mt-1"
            :class="{ 'input-error': errors.riskLevel }"
            @change="touch('riskLevel')"
          >
            <option value="" disabled>Select risk level</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
          <p v-if="errors.riskLevel" class="field-error">
            {{ errors.riskLevel }}
          </p>
        </div>

        <!-- Approval Type -->
        <div>
          <label class="field-label"
            >Approval Type <span class="text-danger">*</span></label
          >
          <select
            v-model="form.approvalType"
            class="input mt-1"
            :class="{ 'input-error': errors.approvalType }"
            @change="touch('approvalType')"
          >
            <option value="" disabled>Select approval type</option>
            <option value="LINEAR">Linear</option>
            <option value="NON_LINEAR">Non Linear</option>
          </select>
          <p v-if="errors.approvalType" class="field-error">
            {{ errors.approvalType }}
          </p>
        </div>

        <!-- Category -->
        <div>
          <label class="field-label">Category</label>
          <input
            v-model="form.category"
            class="input mt-1"
            maxlength="255"
            placeholder="Infrastructure / Application / Security"
          />
        </div>

        <!-- Scheduled Start -->
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
              @update:model-value="onStartDateChange"
            />
          </ClientOnly>
          <p v-if="errors.scheduledStart" class="field-error">
            {{ errors.scheduledStart }}
          </p>
        </div>

        <!-- Scheduled End -->
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
              @update:model-value="touch('scheduledEnd')"
            />
          </ClientOnly>
          <p v-if="errors.scheduledEnd" class="field-error">
            {{ errors.scheduledEnd }}
          </p>
        </div>

        <!-- Affected Systems -->
        <div class="md:col-span-2">
          <label class="field-label">Affected Systems (comma separated)</label>
          <input
            v-model="form.affectedSystemsInput"
            class="input mt-1"
            placeholder="payment-api, nginx-prod, postgres-replica"
          />
        </div>

        <!-- Description -->
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
      <p v-if="errorMessage" class="text-sm text-danger">{{ errorMessage }}</p>
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
  priority: "" as string,
  riskLevel: "" as string,
  approvalType: "" as string,
  category: "",
  scheduledStart: null as Date | null,
  scheduledEnd: null as Date | null,
  affectedSystemsInput: "",
});

// Per-field error messages — only shown after the field has been touched
const errors = reactive<Record<string, string>>({});
const touched = reactive<Record<string, boolean>>({});

function touch(field: string) {
  touched[field] = true;
  validateField(field);
}

function validateField(field: string) {
  switch (field) {
    case "title":
      errors.title = form.title.trim() ? "" : "Change title is required.";
      break;
    case "priority":
      errors.priority = form.priority ? "" : "Priority level is required.";
      break;
    case "riskLevel":
      errors.riskLevel = form.riskLevel ? "" : "Risk assessment is required.";
      break;
    case "approvalType":
      errors.approvalType = form.approvalType
        ? ""
        : "Approval type is required.";
      break;
    case "scheduledStart":
      if (
        form.scheduledEnd &&
        form.scheduledStart &&
        form.scheduledStart >= form.scheduledEnd
      ) {
        errors.scheduledEnd = "End must be after the start date.";
      } else {
        errors.scheduledEnd = "";
      }
      errors.scheduledStart = "";
      break;
    case "scheduledEnd":
      if (
        form.scheduledEnd &&
        form.scheduledStart &&
        form.scheduledEnd <= form.scheduledStart
      ) {
        errors.scheduledEnd = "End must be after the start date.";
      } else {
        errors.scheduledEnd = "";
      }
      break;
  }
}

function onStartDateChange(val: Date | null) {
  // If end is now before new start, clear end to force re-selection
  if (val && form.scheduledEnd && form.scheduledEnd <= val) {
    form.scheduledEnd = null;
    errors.scheduledEnd = "End must be after the start date.";
  }
  touch("scheduledStart");
}

function validateAll(): boolean {
  const requiredFields = ["title", "priority", "riskLevel", "approvalType"];
  requiredFields.forEach((f) => {
    touched[f] = true;
    validateField(f);
  });
  touch("scheduledEnd");
  return !Object.values(errors).some(Boolean);
}

async function createChangeRequest() {
  errorMessage.value = "";
  if (!validateAll()) return;

  isSaving.value = true;
  try {
    const payload = {
      title: form.title.trim(),
      description: editor.value?.getHTML() || null,
      priority: form.priority,
      riskLevel: form.riskLevel,
      approvalType: form.approvalType,
      category: form.category.trim() || null,
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
