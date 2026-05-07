<template>
  <div class="space-y-6">
    <div class="flex items-start justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Administration
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Organization Settings</h1>
        <p class="text-sm text-muted mt-1">
          Configure tenant profile, governance defaults, and notification
          policy.
        </p>
      </div>
      <button type="button" class="btn-primary btn-sm" disabled>
        Save Changes
      </button>
    </div>

    <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
      <section class="card p-5 shadow-card-hover lg:col-span-2 space-y-5">
        <h2 class="text-lg font-semibold">Organization Profile</h2>

        <div v-if="pending" class="field-hint">
          Loading organization profile...
        </div>

        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label class="field-label" for="org-name">Organization Name</label>
            <input
              id="org-name"
              type="text"
              class="input"
              :value="settings.name"
              disabled
            />
          </div>
          <div>
            <label class="field-label" for="org-slug">Organization Slug</label>
            <input
              id="org-slug"
              type="text"
              class="input"
              :value="settings.slug"
              disabled
            />
          </div>
          <div>
            <label class="field-label" for="org-email"
              >Primary Contact Email</label
            >
            <input
              id="org-email"
              type="email"
              class="input"
              :value="settings.email"
              disabled
            />
          </div>
          <div>
            <label class="field-label" for="org-timezone">Time Zone</label>
            <input
              id="org-timezone"
              type="text"
              class="input"
              :value="settings.timezone"
              disabled
            />
          </div>
        </div>

        <p v-if="errorMessage" class="field-hint text-danger">
          {{ errorMessage }}
        </p>
      </section>

      <section class="card p-5 shadow-card-hover space-y-4">
        <h2 class="text-lg font-semibold">Feature Flags</h2>

        <div class="space-y-3 text-sm text-muted">
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Policy breach digests</span>
            <AppBadge
              :variant="
                settings.featureFlags.policyBreachDigests
                  ? 'success'
                  : 'neutral'
              "
            >
              {{
                settings.featureFlags.policyBreachDigests
                  ? "Enabled"
                  : "Disabled"
              }}
            </AppBadge>
          </div>
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Automated reminders</span>
            <AppBadge
              :variant="
                settings.featureFlags.automatedReminders ? 'success' : 'neutral'
              "
            >
              {{
                settings.featureFlags.automatedReminders
                  ? "Enabled"
                  : "Disabled"
              }}
            </AppBadge>
          </div>
          <div
            class="flex items-center justify-between rounded-md border border-border p-3 dark:border-border-dark"
          >
            <span>Conditional escalation</span>
            <AppBadge
              :variant="
                settings.featureFlags.conditionalEscalation
                  ? 'success'
                  : 'neutral'
              "
            >
              {{
                settings.featureFlags.conditionalEscalation
                  ? "Enabled"
                  : "Disabled"
              }}
            </AppBadge>
          </div>
        </div>

        <p v-if="errorMessage" class="field-hint text-danger">
          {{ errorMessage }}
        </p>
      </section>
    </div>

    <section class="card p-5 shadow-card-hover">
      <h2 class="text-lg font-semibold">Security Defaults</h2>
      <div class="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
        <div
          class="rounded-md border border-border p-4 dark:border-border-dark"
        >
          <p class="text-xs uppercase tracking-wide text-muted">
            Session Timeout
          </p>
          <p class="mt-1 text-sm font-semibold">
            {{ settings.securityDefaults.sessionTimeoutLabel }}
          </p>
        </div>
        <div
          class="rounded-md border border-border p-4 dark:border-border-dark"
        >
          <p class="text-xs uppercase tracking-wide text-muted">MFA Policy</p>
          <p class="mt-1 text-sm font-semibold">
            {{ settings.securityDefaults.mfaPolicy }}
          </p>
        </div>
        <div
          class="rounded-md border border-border p-4 dark:border-border-dark"
        >
          <p class="text-xs uppercase tracking-wide text-muted">
            Password Policy
          </p>
          <p class="mt-1 text-sm font-semibold">
            {{ settings.securityDefaults.passwordPolicy }}
          </p>
        </div>
      </div>
    </section>

    <!-- Custom Fields -->
    <section class="card p-5 shadow-card-hover">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-lg font-semibold">Custom Fields</h2>
          <p class="text-sm text-muted mt-0.5">
            Define additional fields that appear on every change request.
          </p>
        </div>
        <button
          type="button"
          class="btn-primary btn-sm"
          aria-label="Add a new custom field"
          @click="openCreateModal"
        >
          Add Field
        </button>
      </div>

      <div v-if="cfPending" class="mt-4 field-hint">Loading custom fields…</div>

      <div
        v-else-if="customFields.length === 0"
        class="mt-4 text-sm text-muted italic"
      >
        No custom fields defined yet.
      </div>

      <div v-else class="mt-4 overflow-x-auto">
        <table class="w-full text-sm" aria-label="Custom field definitions">
          <thead>
            <tr
              class="border-b border-border dark:border-border-dark text-left text-xs uppercase tracking-wide text-muted"
            >
              <th class="py-2 pr-4">Label</th>
              <th class="py-2 pr-4">Type</th>
              <th class="py-2 pr-4">Required</th>
              <th class="py-2 pr-4">Order</th>
              <th class="py-2 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="field in customFields"
              :key="field.id"
              class="border-b border-border/50 dark:border-border-dark/50 hover:bg-surface-hover dark:hover:bg-surface-dark-hover"
            >
              <td class="py-2 pr-4 font-medium">{{ field.label }}</td>
              <td class="py-2 pr-4">
                <AppBadge variant="neutral">{{ field.fieldType }}</AppBadge>
              </td>
              <td class="py-2 pr-4">
                <AppBadge :variant="field.isRequired ? 'warning' : 'neutral'">
                  {{ field.isRequired ? "Required" : "Optional" }}
                </AppBadge>
              </td>
              <td class="py-2 pr-4 text-muted">{{ field.displayOrder }}</td>
              <td class="py-2 text-right">
                <button
                  type="button"
                  class="btn-ghost btn-sm mr-1"
                  :aria-label="`Edit custom field ${field.label}`"
                  @click="openEditModal(field)"
                >
                  Edit
                </button>
                <button
                  type="button"
                  class="btn-ghost btn-sm text-danger"
                  :aria-label="`Delete custom field ${field.label}`"
                  @click="confirmDelete(field)"
                >
                  Delete
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-if="cfError" class="mt-3 text-sm text-danger" role="alert">
        {{ cfError }}
      </p>
    </section>

    <!-- Custom Field Modal -->
    <SharedAppModal
      :open="showCfModal"
      :title="editingField ? 'Edit Custom Field' : 'Add Custom Field'"
      @close="closeCfModal"
    >
      <form class="space-y-4" @submit.prevent="saveCfField">
        <div>
          <label class="field-label" for="cf-label">Label</label>
          <input
            id="cf-label"
            v-model="cfForm.label"
            type="text"
            class="input"
            required
            maxlength="255"
            placeholder="e.g. Business Justification"
          />
        </div>
        <div>
          <label class="field-label" for="cf-type">Field Type</label>
          <select id="cf-type" v-model="cfForm.fieldType" class="input">
            <option value="TEXT">Text</option>
            <option value="NUMBER">Number</option>
            <option value="DATE">Date</option>
            <option value="DROPDOWN">Dropdown</option>
            <option value="CHECKBOX">Checkbox</option>
          </select>
        </div>
        <div v-if="cfForm.fieldType === 'DROPDOWN'">
          <label class="field-label" for="cf-options">
            Options
            <span class="text-muted font-normal">(one per line)</span>
          </label>
          <textarea
            id="cf-options"
            v-model="cfForm.optionsText"
            class="input min-h-[80px]"
            placeholder="Option A&#10;Option B&#10;Option C"
          />
        </div>
        <div class="flex items-center gap-2">
          <input
            id="cf-required"
            v-model="cfForm.isRequired"
            type="checkbox"
            class="rounded border-border"
          />
          <label for="cf-required" class="text-sm">Required field</label>
        </div>
        <div>
          <label class="field-label" for="cf-order">Display Order</label>
          <input
            id="cf-order"
            v-model.number="cfForm.displayOrder"
            type="number"
            class="input"
            min="0"
            placeholder="0"
          />
        </div>
        <p v-if="cfSaveError" class="text-sm text-danger" role="alert">
          {{ cfSaveError }}
        </p>
        <div class="flex justify-end gap-2 pt-2">
          <button
            type="button"
            class="btn-secondary btn-sm"
            @click="closeCfModal"
          >
            Cancel
          </button>
          <button type="submit" class="btn-primary btn-sm" :disabled="cfSaving">
            {{ cfSaving ? "Saving…" : editingField ? "Update" : "Create" }}
          </button>
        </div>
      </form>
    </SharedAppModal>

    <!-- Delete Confirm Modal -->
    <SharedAppModal
      :open="showDeleteModal"
      title="Delete Custom Field"
      @close="showDeleteModal = false"
    >
      <p class="text-sm text-muted mb-4">
        Are you sure you want to delete
        <strong>{{ deletingField?.label }}</strong
        >? This will not remove values already saved on existing change
        requests.
      </p>
      <div class="flex justify-end gap-2">
        <button
          type="button"
          class="btn-secondary btn-sm"
          @click="showDeleteModal = false"
        >
          Cancel
        </button>
        <button
          type="button"
          class="btn-danger btn-sm"
          :disabled="cfDeleting"
          @click="executeDelete"
        >
          {{ cfDeleting ? "Deleting…" : "Delete" }}
        </button>
      </div>
    </SharedAppModal>
  </div>
</template>

<script setup lang="ts">
import type { CustomFieldDefinition } from "~/types";

definePageMeta({ layout: "default" });

const api = useApi();
const { error: toastError } = useToast();

interface TenantAdminSettingsResponse {
  profile: {
    name: string;
    slug: string;
    primaryContactEmail: string | null;
    timezone: string;
    status: string;
  };
  featureFlags: {
    policyBreachDigests: boolean;
    automatedReminders: boolean;
    conditionalEscalation: boolean;
  };
  securityDefaults: {
    sessionTimeoutMinutes: number | null;
    mfaPolicy: string;
    passwordPolicy: string;
  };
}

const pending = ref(false);
const errorMessage = ref("");

const settings = reactive({
  name: "",
  slug: "",
  email: "Not configured",
  timezone: "UTC",
  featureFlags: {
    policyBreachDigests: false,
    automatedReminders: false,
    conditionalEscalation: false,
  },
  securityDefaults: {
    sessionTimeoutLabel: "Not configured",
    mfaPolicy: "Not configured",
    passwordPolicy: "Not configured",
  },
});

async function loadSettings() {
  pending.value = true;
  errorMessage.value = "";
  try {
    const response = await api<TenantAdminSettingsResponse>(
      "/api/v1/settings",
      {
        method: "GET",
      },
    );
    settings.name = response.profile.name;
    settings.slug = response.profile.slug;
    settings.email = response.profile.primaryContactEmail ?? "Not configured";
    settings.timezone = response.profile.timezone || "UTC";
    settings.featureFlags = response.featureFlags;
    settings.securityDefaults.sessionTimeoutLabel = response.securityDefaults
      .sessionTimeoutMinutes
      ? `${response.securityDefaults.sessionTimeoutMinutes} minutes`
      : "Not configured";
    settings.securityDefaults.mfaPolicy = response.securityDefaults.mfaPolicy;
    settings.securityDefaults.passwordPolicy =
      response.securityDefaults.passwordPolicy;
  } catch {
    errorMessage.value = "Unable to load settings right now.";
    toastError("Failed to load organization settings.");
  } finally {
    pending.value = false;
  }
}

onMounted(loadSettings);

// ── Custom Fields ─────────────────────────────────────────────────────────────

const customFields = ref<CustomFieldDefinition[]>([]);
const cfPending = ref(false);
const cfError = ref("");
const showCfModal = ref(false);
const showDeleteModal = ref(false);
const editingField = ref<CustomFieldDefinition | null>(null);
const deletingField = ref<CustomFieldDefinition | null>(null);
const cfSaving = ref(false);
const cfDeleting = ref(false);
const cfSaveError = ref("");

const cfForm = reactive({
  label: "",
  fieldType: "TEXT",
  optionsText: "",
  isRequired: false,
  displayOrder: 0,
});

async function loadCustomFields() {
  cfPending.value = true;
  cfError.value = "";
  try {
    customFields.value = await api<CustomFieldDefinition[]>(
      "/api/v1/admin/custom-fields",
    );
  } catch {
    cfError.value = "Unable to load custom fields.";
  } finally {
    cfPending.value = false;
  }
}

function openCreateModal() {
  editingField.value = null;
  cfForm.label = "";
  cfForm.fieldType = "TEXT";
  cfForm.optionsText = "";
  cfForm.isRequired = false;
  cfForm.displayOrder = customFields.value.length;
  cfSaveError.value = "";
  showCfModal.value = true;
}

function openEditModal(field: CustomFieldDefinition) {
  editingField.value = field;
  cfForm.label = field.label;
  cfForm.fieldType = field.fieldType;
  cfForm.optionsText = (field.options ?? []).join("\n");
  cfForm.isRequired = field.isRequired;
  cfForm.displayOrder = field.displayOrder;
  cfSaveError.value = "";
  showCfModal.value = true;
}

function closeCfModal() {
  showCfModal.value = false;
  editingField.value = null;
}

async function saveCfField() {
  cfSaving.value = true;
  cfSaveError.value = "";
  const options =
    cfForm.fieldType === "DROPDOWN"
      ? cfForm.optionsText
          .split("\n")
          .map((s) => s.trim())
          .filter(Boolean)
      : null;

  const body = {
    label: cfForm.label.trim(),
    fieldType: cfForm.fieldType,
    options,
    isRequired: cfForm.isRequired,
    displayOrder: cfForm.displayOrder,
  };

  try {
    if (editingField.value) {
      await api(`/api/v1/admin/custom-fields/${editingField.value.id}`, {
        method: "PUT",
        body,
      });
    } else {
      await api("/api/v1/admin/custom-fields", { method: "POST", body });
    }
    closeCfModal();
    await loadCustomFields();
  } catch {
    cfSaveError.value = "Failed to save custom field. Please check your input.";
  } finally {
    cfSaving.value = false;
  }
}

function confirmDelete(field: CustomFieldDefinition) {
  deletingField.value = field;
  showDeleteModal.value = true;
}

async function executeDelete() {
  if (!deletingField.value) return;
  cfDeleting.value = true;
  try {
    await api(`/api/v1/admin/custom-fields/${deletingField.value.id}`, {
      method: "DELETE",
    });
    showDeleteModal.value = false;
    deletingField.value = null;
    await loadCustomFields();
  } catch {
    cfError.value = "Failed to delete custom field.";
    showDeleteModal.value = false;
  } finally {
    cfDeleting.value = false;
  }
}

onMounted(loadCustomFields);
</script>
