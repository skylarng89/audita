<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs text-primary/70 uppercase tracking-[0.16em] font-semibold mb-1"
        >
          Administration
        </p>
        <h1
          class="text-4xl font-bold tracking-tight text-gray-900 dark:text-gray-100"
        >
          Custom Fields
        </h1>
        <p class="text-sm text-muted mt-1">
          Define the custom fields that appear on every change request.
        </p>
      </div>
      <button class="btn-primary btn-md" @click="openCreateModal">
        <svg
          class="w-4 h-4"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M12 4v16m8-8H4"
          />
        </svg>
        New Field
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="space-y-3 pt-2">
      <div v-for="n in 4" :key="n" class="card px-5 py-4 flex items-center justify-between">
        <div class="space-y-2">
          <SharedFieldSkeleton heightClass="h-5" class="w-40" />
          <SharedFieldSkeleton heightClass="h-4" class="w-56" />
        </div>
        <div class="flex items-center gap-2">
          <SharedFieldSkeleton heightClass="h-8" class="w-12" />
          <SharedFieldSkeleton heightClass="h-8" class="w-14" />
        </div>
      </div>
    </div>

    <!-- Error -->
    <p v-else-if="loadError" class="text-sm text-danger">{{ loadError }}</p>

    <!-- Empty state -->
    <div v-else-if="!fields.length" class="card p-10 text-center space-y-3">
      <p class="text-sm font-medium text-on-surface">No custom fields yet</p>
      <p class="text-sm text-muted">
        Add custom fields to capture additional metadata on every change
        request.
      </p>
      <button class="btn-primary btn-md" @click="openCreateModal">
        Add First Field
      </button>
    </div>

    <!-- Field list -->
    <div
      v-else
      class="card divide-y divide-outline-variant/30 dark:divide-border-dark"
    >
      <div
        v-for="field in fields"
        :key="field.id"
        class="flex items-center justify-between px-5 py-4"
      >
        <div class="space-y-0.5">
          <div class="flex items-center gap-2">
            <p class="text-sm font-semibold">{{ field.label }}</p>
            <span
              v-if="field.isRequired"
              class="text-[10px] font-semibold uppercase tracking-wider text-danger bg-danger/10 px-1.5 py-0.5 rounded"
              >Required</span
            >
          </div>
          <p class="text-xs text-muted">
            {{ fieldTypeLabel(field.fieldType) }}
            <template v-if="field.options?.length">
              · {{ field.options.join(", ") }}
            </template>
          </p>
        </div>
        <div class="flex items-center gap-2">
          <button class="btn-ghost btn-sm" @click="openEditModal(field)">
            Edit
          </button>
          <button
            class="btn-ghost btn-sm text-danger hover:bg-danger/10"
            @click="confirmDelete(field)"
          >
            Delete
          </button>
        </div>
      </div>
    </div>

    <!-- Create / Edit modal -->
    <div
      v-if="showModal"
      class="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50"
    >
      <div class="card p-6 max-w-md w-full space-y-4">
        <h3 class="font-semibold">
          {{ editingField ? "Edit Field" : "New Custom Field" }}
        </h3>

        <div class="space-y-4">
          <div>
            <label class="field-label"
              >Label <span class="text-danger">*</span></label
            >
            <input
              v-model="form.label"
              class="input mt-1"
              placeholder="e.g., Change Category"
              maxlength="255"
            />
          </div>

          <div>
            <label class="field-label"
              >Field Type <span class="text-danger">*</span></label
            >
            <select v-model="form.fieldType" class="input mt-1">
              <option value="TEXT">Text</option>
              <option value="NUMBER">Number</option>
              <option value="DATE">Date</option>
              <option value="DROPDOWN">Dropdown</option>
              <option value="CHECKBOX">Checkbox</option>
            </select>
          </div>

          <div v-if="form.fieldType === 'DROPDOWN'">
            <label class="field-label"
              >Options (one per line) <span class="text-danger">*</span></label
            >
            <textarea
              v-model="form.optionsText"
              class="input mt-1"
              rows="4"
              placeholder="Standard&#10;Emergency&#10;Maintenance"
            />
          </div>

          <div>
            <label class="field-label">Display Order</label>
            <input
              v-model.number="form.displayOrder"
              type="number"
              class="input mt-1"
              min="0"
            />
          </div>

          <label class="flex items-center gap-2 text-sm cursor-pointer">
            <input
              v-model="form.isRequired"
              type="checkbox"
              class="h-4 w-4 accent-primary"
            />
            Required field
          </label>
        </div>

        <p v-if="saveError" class="text-sm text-danger">{{ saveError }}</p>

        <div class="flex justify-end gap-2 pt-2">
          <button class="btn-ghost btn-md" @click="closeModal">Cancel</button>
          <button
            class="btn-primary btn-md"
            :disabled="saving"
            @click="saveField"
          >
            {{
              saving
                ? "Saving…"
                : editingField
                  ? "Save Changes"
                  : "Create Field"
            }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete confirmation modal -->
    <div
      v-if="showDeleteConfirm"
      class="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50"
    >
      <div class="card p-6 max-w-sm w-full space-y-4">
        <h3 class="font-semibold">Delete Field</h3>
        <p class="text-sm text-muted">
          Are you sure you want to delete
          <strong>{{ deletingField?.label }}</strong
          >? This will remove saved values on all change requests.
        </p>
        <div class="flex justify-end gap-2">
          <button class="btn-ghost btn-md" @click="showDeleteConfirm = false">
            Cancel
          </button>
          <button
            class="btn-primary btn-md text-danger"
            :disabled="deleting"
            @click="deleteField"
          >
            {{ deleting ? "Deleting…" : "Delete" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CustomFieldDefinition } from "~/types";

definePageMeta({ middleware: ["auth", "admin-only"] });

useHead({ title: "Custom Fields — Audita" });

const api = useApi();

const fields = ref<CustomFieldDefinition[]>([]);
const loading = ref(true);
const loadError = ref("");

// Modal state
const showModal = ref(false);
const editingField = ref<CustomFieldDefinition | null>(null);
const saving = ref(false);
const saveError = ref("");

const form = reactive({
  label: "",
  fieldType: "TEXT" as CustomFieldDefinition["fieldType"],
  optionsText: "",
  isRequired: false,
  displayOrder: 0,
});

// Delete state
const showDeleteConfirm = ref(false);
const deletingField = ref<CustomFieldDefinition | null>(null);
const deleting = ref(false);

async function loadFields() {
  loading.value = true;
  loadError.value = "";
  try {
    fields.value = await api<CustomFieldDefinition[]>(
      "/api/v1/admin/custom-fields",
    );
  } catch {
    loadError.value = "Unable to load custom fields. Please try again.";
  } finally {
    loading.value = false;
  }
}

function fieldTypeLabel(type: string): string {
  return (
    {
      TEXT: "Text",
      NUMBER: "Number",
      DATE: "Date",
      DROPDOWN: "Dropdown",
      CHECKBOX: "Checkbox",
    }[type] ?? type
  );
}

function openCreateModal() {
  editingField.value = null;
  form.label = "";
  form.fieldType = "TEXT";
  form.optionsText = "";
  form.isRequired = false;
  form.displayOrder = fields.value.length;
  saveError.value = "";
  showModal.value = true;
}

function openEditModal(field: CustomFieldDefinition) {
  editingField.value = field;
  form.label = field.label;
  form.fieldType = field.fieldType;
  form.optionsText = (field.options ?? []).join("\n");
  form.isRequired = field.isRequired;
  form.displayOrder = field.displayOrder;
  saveError.value = "";
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  editingField.value = null;
}

async function saveField() {
  if (!form.label.trim()) {
    saveError.value = "Label is required.";
    return;
  }
  if (form.fieldType === "DROPDOWN" && !form.optionsText.trim()) {
    saveError.value = "At least one option is required for dropdown fields.";
    return;
  }

  const options =
    form.fieldType === "DROPDOWN"
      ? form.optionsText
          .split("\n")
          .map((s) => s.trim())
          .filter(Boolean)
      : null;

  const body = {
    label: form.label.trim(),
    fieldType: form.fieldType,
    options,
    isRequired: form.isRequired,
    displayOrder: form.displayOrder,
  };

  saving.value = true;
  saveError.value = "";
  try {
    if (editingField.value) {
      await api(`/api/v1/admin/custom-fields/${editingField.value.id}`, {
        method: "PUT",
        body,
      });
    } else {
      await api("/api/v1/admin/custom-fields", { method: "POST", body });
    }
    closeModal();
    await loadFields();
  } catch {
    saveError.value = "Failed to save. Please check your input and try again.";
  } finally {
    saving.value = false;
  }
}

function confirmDelete(field: CustomFieldDefinition) {
  deletingField.value = field;
  showDeleteConfirm.value = true;
}

async function deleteField() {
  if (!deletingField.value) return;
  deleting.value = true;
  try {
    await api(`/api/v1/admin/custom-fields/${deletingField.value.id}`, {
      method: "DELETE",
    });
    showDeleteConfirm.value = false;
    deletingField.value = null;
    await loadFields();
  } catch {
    loadError.value = "Failed to delete field. Please try again.";
    showDeleteConfirm.value = false;
  } finally {
    deleting.value = false;
  }
}

onMounted(loadFields);
</script>
