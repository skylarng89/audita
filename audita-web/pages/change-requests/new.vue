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
        <div class="relative" ref="categoryWrapperRef">
          <label class="field-label">Category</label>
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
          <label class="field-label">Scheduled Start</label>
          <div class="mt-1 grid grid-cols-2 gap-2">
            <ClientOnly>
              <VueDatePicker
                v-model="form.scheduledStartDate"
                :enable-time-picker="false"
                model-type="yyyy-MM-dd"
                placeholder="Date"
                format="dd MMM yyyy"
                auto-apply
                teleport="body"
                :dark="isDark"
                @update:model-value="onStartChange"
              />
            </ClientOnly>
            <ClientOnly>
              <VueDatePicker
                v-model="form.scheduledStartTime"
                time-picker
                placeholder="Time"
                :dark="isDark"
                @update:model-value="onStartChange"
              />
            </ClientOnly>
          </div>
          <p v-if="errors.scheduledStart" class="field-error">
            {{ errors.scheduledStart }}
          </p>
        </div>

        <!-- Scheduled End -->
        <div>
          <label class="field-label">Scheduled End</label>
          <div class="mt-1 grid grid-cols-2 gap-2">
            <ClientOnly>
              <VueDatePicker
                v-model="form.scheduledEndDate"
                :enable-time-picker="false"
                model-type="yyyy-MM-dd"
                placeholder="Date"
                :min-date="form.scheduledStartDate ?? undefined"
                format="dd MMM yyyy"
                auto-apply
                teleport="body"
                :dark="isDark"
                @update:model-value="touch('scheduledEnd')"
              />
            </ClientOnly>
            <ClientOnly>
              <VueDatePicker
                v-model="form.scheduledEndTime"
                time-picker
                placeholder="Time"
                :dark="isDark"
                @update:model-value="touch('scheduledEnd')"
              />
            </ClientOnly>
          </div>
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

const { create, listCategories } = useChangeRequests();

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
  categories: [] as string[],
  scheduledStartDate: "" as string,
  scheduledStartTime: null as { hours: number; minutes: number } | null,
  scheduledEndDate: "" as string,
  scheduledEndTime: null as { hours: number; minutes: number } | null,
  affectedSystemsInput: "",
});

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

// ── Dark mode detection for VueDatePicker ──────────────────────────────────
const isDark = ref(false);

// Close dropdown when clicking outside
onMounted(async () => {
  isDark.value = document.documentElement.classList.contains("dark");
  const darkObserver = new MutationObserver(() => {
    isDark.value = document.documentElement.classList.contains("dark");
  });
  darkObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["class"],
  });
  onUnmounted(() => darkObserver.disconnect());

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

function combineParts(
  dateStr: string,
  time: { hours: number; minutes: number } | null,
): Date | null {
  if (!dateStr) return null;
  const [year, month, day] = dateStr.split("-").map(Number);
  return new Date(
    year,
    month - 1,
    day,
    time?.hours ?? 0,
    time?.minutes ?? 0,
    0,
    0,
  );
}

function onStartChange() {
  const start = combineParts(form.scheduledStartDate, form.scheduledStartTime);
  const end = combineParts(form.scheduledEndDate, form.scheduledEndTime);
  if (start && end && end <= start) {
    form.scheduledEndDate = "";
    form.scheduledEndTime = null;
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
