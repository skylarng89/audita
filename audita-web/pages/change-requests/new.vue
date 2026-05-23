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
          class="text-3xl font-bold tracking-tight text-gray-900 dark:text-gray-100"
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
          <p class="field-label">
            Change Title <span class="text-danger">*</span>
          </p>
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
          <p class="field-label">
            Priority Level <span class="text-danger">*</span>
          </p>
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
          <p class="field-label">
            Risk Assessment <span class="text-danger">*</span>
          </p>
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
          <p class="field-label">
            Approval Type <span class="text-danger">*</span>
          </p>
          <select
            v-model="form.approvalType"
            class="input mt-1"
            :class="{ 'input-error': errors.approvalType }"
            @change="touch('approvalType')"
          >
            <option value="" disabled>Select approval type</option>
            <option value="LINEAR">Linear</option>
            <option value="NON_LINEAR">Non-Linear</option>
          </select>
          <p v-if="errors.approvalType" class="field-error">
            {{ errors.approvalType }}
          </p>
        </div>

        <!-- Category -->
        <div class="relative" ref="categoryWrapperRef">
          <p class="field-label">Category</p>
          <!-- Selected tags + search input -->
          <div
            class="input mt-1 flex flex-wrap gap-1 min-h-[2.5rem] cursor-text"
            :class="{ 'ring-2 ring-primary': categoryOpen }"
            @click="openCategory"
          >
            <span
              v-for="tag in form.categories"
              :key="tag"
              class="inline-flex items-center gap-1 rounded bg-primary/10 text-primary text-xs px-2 py-0.5"
            >
              {{ tag }}
              <button
                type="button"
                class="hover:text-danger leading-none"
                @click.stop="removeCategory(tag)"
                :aria-label="`Remove ${tag}`"
              >
                ×
              </button>
            </span>
            <input
              ref="categoryInputRef"
              v-model="categorySearch"
              class="flex-1 min-w-[8rem] bg-transparent outline-none text-sm"
              placeholder="Search or add…"
              @focus="categoryOpen = true"
              @keydown.enter.prevent="addCategoryFromInput"
              @keydown.backspace="onCategoryBackspace"
            />
          </div>
          <!-- Dropdown -->
          <ul
            v-if="categoryOpen && filteredCategories.length"
            class="absolute z-50 mt-1 w-full bg-surface border border-border dark:border-border-dark rounded-lg shadow-lg max-h-52 overflow-y-auto"
          >
            <li
              v-for="cat in filteredCategories"
              :key="cat"
              class="px-3 py-2 text-sm cursor-pointer hover:bg-primary/10 flex items-center justify-between"
              @mousedown.prevent="selectCategory(cat)"
            >
              {{ cat }}
              <span
                v-if="form.categories.includes(cat)"
                class="text-primary text-xs"
                >✓</span
              >
            </li>
          </ul>
        </div>

        <!-- Scheduled Start -->
        <div>
          <p class="field-label">Scheduled Start</p>
          <div class="mt-1 grid grid-cols-2 gap-2">
            <FlatPickr
              v-model="form.scheduledStartDate"
              :config="datePickerConfig"
              class="input"
            />
            <FlatPickr
              v-model="form.scheduledStartTime"
              :config="timePickerConfig"
              class="input"
            />
          </div>
          <p v-if="errors.scheduledStart" class="field-error">
            {{ errors.scheduledStart }}
          </p>
        </div>

        <!-- Scheduled End -->
        <div>
          <p class="field-label">Scheduled End</p>
          <div class="mt-1 grid grid-cols-2 gap-2">
            <FlatPickr
              v-model="form.scheduledEndDate"
              :config="endDatePickerConfig"
              class="input"
            />
            <FlatPickr
              v-model="form.scheduledEndTime"
              :config="timePickerConfig"
              class="input"
            />
          </div>
          <p v-if="errors.scheduledEnd" class="field-error">
            {{ errors.scheduledEnd }}
          </p>
        </div>

        <!-- Affected Systems -->
        <div class="md:col-span-2">
          <p class="field-label">Affected Systems</p>
          <div
            class="input mt-1 flex flex-wrap gap-1 min-h-[2.5rem] cursor-text"
            @click="affectedSystemsInputRef?.focus()"
          >
            <span
              v-for="sys in form.affectedSystems"
              :key="sys"
              class="inline-flex items-center gap-1 rounded bg-primary/10 text-primary text-xs px-2 py-0.5 shrink-0"
            >
              {{ sys }}
              <button
                type="button"
                class="hover:text-danger leading-none"
                :aria-label="`Remove ${sys}`"
                @click.stop="removeAffectedSystem(sys)"
              >
                ×
              </button>
            </span>
            <input
              ref="affectedSystemsInputRef"
              v-model="affectedSystemsTagInput"
              class="flex-1 min-w-[10rem] bg-transparent outline-none text-sm"
              placeholder="Add system and press Enter…"
              @keydown.enter.prevent="addAffectedSystem"
              @keydown="onAffectedSystemsKeydown"
              @keydown.backspace="onAffectedSystemsBackspace"
            />
          </div>
          <p class="field-hint">Press Enter or comma to add each system.</p>
        </div>

        <!-- Description -->
        <div class="md:col-span-2">
          <p class="field-label">Scope and Description</p>
          <div class="rich-editor-shell">
            <ClientOnly>
              <SharedRichTextToolbar :editor="editor" />
              <EditorContent :editor="editor" class="rich-editor-content" />
            </ClientOnly>
          </div>
          <p class="field-hint">
            Define technical scope, constraints, and rollback strategy.
          </p>
        </div>

        <div class="md:col-span-2">
          <p class="field-label">Attachments</p>
          <div
            class="rounded-lg border border-dashed border-outline-variant/70 bg-surface-container-low p-5 dark:border-slate-600 dark:bg-slate-800/70"
          >
            <div
              class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
            >
              <div>
                <p
                  class="text-sm font-medium text-on-surface dark:text-gray-100"
                >
                  Add files before creating the draft
                </p>
                <p class="text-xs text-muted">
                  Files upload automatically after the draft is created.
                </p>
              </div>
              <div class="flex items-center gap-2">
                <input
                  ref="pendingFileInput"
                  class="hidden"
                  type="file"
                  multiple
                  accept=".png,.jpg,.jpeg,.docx,.xlsx,.pdf"
                  @change="onSelectPendingFiles"
                />
                <button
                  type="button"
                  class="btn-ghost btn-md"
                  @click="pendingFileInput?.click()"
                >
                  Select Files
                </button>
              </div>
            </div>
            <p v-if="uploadError" class="field-error">{{ uploadError }}</p>
            <div class="mt-4 space-y-2" v-if="pendingAttachments.length">
              <div
                v-for="file in pendingAttachments"
                :key="`${file.name}-${file.size}-${file.lastModified}`"
                class="flex items-center justify-between rounded-lg border border-outline-variant/50 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-900/50"
              >
                <div>
                  <p class="font-medium text-on-surface dark:text-gray-100">
                    {{ file.name }}
                  </p>
                  <p class="text-xs text-muted">{{ formatSize(file.size) }}</p>
                </div>
                <button
                  type="button"
                  class="btn-ghost btn-sm"
                  @click="removePendingAttachment(file)"
                >
                  Remove
                </button>
              </div>
            </div>
          </div>
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
import FlatPickr from "vue-flatpickr-component";
import { buildRichTextExtensions } from "~/composables/richText";

definePageMeta({ middleware: ["auth", "can-create-cr"] });

useHead({ title: "Create Change Request — Audita" });

const { create, listCategories, uploadAttachment } = useChangeRequests();
const { error: toastError } = useToast();

const isSaving = ref(false);
const errorMessage = ref("");

const editor = useEditor({
  content: "<p></p>",
  extensions: buildRichTextExtensions("Describe scope, rollout plan, and risk controls..."),
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
  approvalType: "NON_LINEAR" as string,
  categories: [] as string[],
  scheduledStartDate: "",
  scheduledStartTime: "",
  scheduledEndDate: "",
  scheduledEndTime: "",
  affectedSystems: [] as string[],
});

const pendingFileInput = ref<HTMLInputElement | null>(null);
const pendingAttachments = ref<File[]>([]);
const uploadError = ref("");

const ALLOWED_EXTENSIONS = new Set([
  "png",
  "jpg",
  "jpeg",
  "docx",
  "xlsx",
  "pdf",
]);
const ALLOWED_MIME_TYPES = new Set([
  "image/png",
  "image/jpeg",
  "application/pdf",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
]);

function isFileTypeAllowed(file: File): boolean {
  const extension = file.name.split(".").pop()?.toLowerCase() ?? "";
  return ALLOWED_EXTENSIONS.has(extension) && ALLOWED_MIME_TYPES.has(file.type);
}

function addPendingFiles(files: File[]) {
  const nextFiles: File[] = [];
  for (const file of files) {
    if (!isFileTypeAllowed(file)) {
      uploadError.value =
        "Only PNG, JPG, DOCX, XLSX, and PDF files are permitted.";
      continue;
    }

    const alreadyQueued = pendingAttachments.value.some(
      (queuedFile) =>
        queuedFile.name === file.name &&
        queuedFile.size === file.size &&
        queuedFile.lastModified === file.lastModified,
    );
    if (!alreadyQueued) {
      nextFiles.push(file);
    }
  }

  if (nextFiles.length) {
    uploadError.value = "";
    pendingAttachments.value = [...pendingAttachments.value, ...nextFiles];
  }
}

function onSelectPendingFiles(event: Event) {
  const target = event.target as HTMLInputElement;
  addPendingFiles(Array.from(target.files ?? []));
  target.value = "";
}

function removePendingAttachment(fileToRemove: File) {
  pendingAttachments.value = pendingAttachments.value.filter(
    (file) =>
      !(
        file.name === fileToRemove.name &&
        file.size === fileToRemove.size &&
        file.lastModified === fileToRemove.lastModified
      ),
  );
}

async function uploadPendingAttachments(changeRequestId: string) {
  for (const file of pendingAttachments.value) {
    await uploadAttachment(changeRequestId, file);
  }
}

function formatSize(bytes: number) {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// ── Affected systems tag input ────────────────────────────────────────────
const affectedSystemsInputRef = ref<HTMLInputElement | null>(null);
const affectedSystemsTagInput = ref("");

function addAffectedSystem() {
  const val = affectedSystemsTagInput.value.replaceAll(",", "").trim();
  if (val && !form.affectedSystems.includes(val)) {
    form.affectedSystems.push(val);
  }
  affectedSystemsTagInput.value = "";
}

function removeAffectedSystem(sys: string) {
  form.affectedSystems = form.affectedSystems.filter((s) => s !== sys);
}

function onAffectedSystemsKeydown(e: KeyboardEvent) {
  if (e.key === ",") {
    e.preventDefault();
    addAffectedSystem();
  }
}

function onAffectedSystemsBackspace() {
  if (!affectedSystemsTagInput.value && form.affectedSystems.length) {
    form.affectedSystems.pop();
  }
}

// ── Category combobox ─────────────────────────────────────────────────────
const allCategories = ref<string[]>([]);
const categorySearch = ref("");
const categoryOpen = ref(false);
const categoryWrapperRef = ref<HTMLElement | null>(null);
const categoryInputRef = ref<HTMLInputElement | null>(null);

const filteredCategories = computed(() => {
  const q = categorySearch.value.trim().toLowerCase();
  const available = allCategories.value.filter(
    (c) => !form.categories.includes(c),
  );
  return q ? available.filter((c) => c.toLowerCase().includes(q)) : available;
});

function openCategory() {
  categoryOpen.value = true;
  nextTick(() => categoryInputRef.value?.focus());
}

function selectCategory(cat: string) {
  if (!form.categories.includes(cat)) {
    form.categories.push(cat);
  }
  categorySearch.value = "";
}

function removeCategory(cat: string) {
  form.categories = form.categories.filter((c) => c !== cat);
}

function addCategoryFromInput() {
  const val = categorySearch.value.trim();
  if (val && !form.categories.includes(val)) {
    form.categories.push(val);
    // Make it available as a future option within this session
    if (!allCategories.value.includes(val)) {
      allCategories.value.push(val);
    }
  }
  categorySearch.value = "";
}

function onCategoryBackspace() {
  if (!categorySearch.value && form.categories.length) {
    form.categories.pop();
  }
}

const datePickerConfig = {
  dateFormat: "Y-m-d",
  allowInput: false,
  clickOpens: true,
  disableMobile: true,
};

const timePickerConfig = {
  enableTime: true,
  noCalendar: true,
  dateFormat: "H:i",
  time_24hr: true,
  allowInput: false,
  clickOpens: true,
  disableMobile: true,
};

const endDatePickerConfig = computed(() => ({
  ...datePickerConfig,
  minDate: form.scheduledStartDate || undefined,
}));

// Close dropdown when clicking outside
onMounted(async () => {
  allCategories.value = await listCategories().catch(() => []);

  document.addEventListener("click", (e) => {
    if (
      categoryWrapperRef.value &&
      !categoryWrapperRef.value.contains(e.target as Node)
    ) {
      categoryOpen.value = false;
    }
  });
});

watch(
  () => [form.scheduledStartDate, form.scheduledStartTime],
  () => onStartChange(),
);

watch(
  () => [form.scheduledEndDate, form.scheduledEndTime],
  () => touch("scheduledEnd"),
);

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
    case "scheduledEnd": {
      const s = combineParts(form.scheduledStartDate, form.scheduledStartTime);
      const e = combineParts(form.scheduledEndDate, form.scheduledEndTime);
      if (s && e && e <= s) {
        errors.scheduledEnd = "End must be after the start date.";
      } else {
        errors.scheduledEnd = "";
      }
      errors.scheduledStart = "";
      break;
    }
  }
}

function combineParts(dateStr: string, timeStr: string): Date | null {
  if (!dateStr) return null;
  const [year, month, day] = dateStr.split("-").map(Number);
  const [hours, minutes] = timeStr ? timeStr.split(":").map(Number) : [0, 0];
  return new Date(year, month - 1, day, hours, minutes, 0, 0);
}

function onStartChange() {
  const start = combineParts(form.scheduledStartDate, form.scheduledStartTime);
  const end = combineParts(form.scheduledEndDate, form.scheduledEndTime);
  if (start && end && end <= start) {
    form.scheduledEndDate = "";
    form.scheduledEndTime = "";
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
      category: form.categories.length ? form.categories.join(", ") : null,
      scheduledStart:
        combineParts(
          form.scheduledStartDate,
          form.scheduledStartTime,
        )?.toISOString() ?? null,
      scheduledEnd:
        combineParts(
          form.scheduledEndDate,
          form.scheduledEndTime,
        )?.toISOString() ?? null,
      affectedSystems: form.affectedSystems,
    };
    const created = await create(payload);
    if (pendingAttachments.value.length) {
      try {
        await uploadPendingAttachments(created.id);
      } catch (error: any) {
        toastError(
          resolveApiErrorMessage(
            error,
            "Change request created, but one or more attachments could not be uploaded.",
          ),
        );
      }
    }
    await navigateTo(`/change-requests/${created.id}`);
  } catch (error: any) {
    errorMessage.value = resolveApiErrorMessage(
      error,
      "Unable to create change request.",
    );
  } finally {
    isSaving.value = false;
  }
}

onBeforeUnmount(() => {
  editor.value?.destroy();
});
</script>
