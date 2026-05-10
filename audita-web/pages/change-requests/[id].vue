<template>
  <div v-if="changeRequest" class="space-y-6">
    <div class="flex items-start justify-between">
      <div>
        <p class="text-xs text-muted uppercase tracking-widest mb-1">
          Change Request
        </p>
        <h1
          class="text-4xl font-bold tracking-tight text-gray-900 dark:text-gray-100"
        >
          {{ changeRequest.title }}
        </h1>
        <p class="text-sm text-muted mt-1">
          Created by {{ changeRequest.createdByFullName ?? "Unknown" }}
        </p>
      </div>
      <div class="flex items-center gap-2">
        <CrStatusBadge :status="changeRequest.status" />
        <CrPriorityBadge :priority="changeRequest.priority" />
        <template v-if="!isEditing">
          <button
            v-if="changeRequest.status === 'DRAFT'"
            class="btn-ghost btn-md"
            @click="enterEditMode"
          >
            Edit
          </button>
        </template>
        <template v-else>
          <button
            class="btn-primary btn-md"
            :disabled="isSaving"
            @click="saveEditAction"
          >
            {{ isSaving ? "Saving…" : "Save" }}
          </button>
          <button class="btn-ghost btn-md" @click="cancelEdit">Cancel</button>
        </template>
      </div>
    </div>

    <div class="card p-4 flex flex-wrap gap-2">
      <button
        class="btn-ghost btn-md"
        :class="{
          'ring-2 ring-primary bg-primary/10 text-primary': tab === 'details',
        }"
        @click="tab = 'details'"
      >
        Details
      </button>
      <button
        class="btn-ghost btn-md"
        :class="{
          'ring-2 ring-primary bg-primary/10 text-primary': tab === 'approvers',
        }"
        @click="tab = 'approvers'"
      >
        Approvers
      </button>
      <button
        class="btn-ghost btn-md"
        :class="{
          'ring-2 ring-primary bg-primary/10 text-primary': tab === 'activity',
        }"
        @click="tab = 'activity'"
      >
        Activity
      </button>
      <button
        class="btn-ghost btn-md"
        :class="{
          'ring-2 ring-primary bg-primary/10 text-primary': tab === 'comments',
        }"
        @click="tab = 'comments'"
      >
        Comments
      </button>
    </div>

    <section
      v-if="tab === 'details'"
      class="grid grid-cols-1 md:grid-cols-2 gap-4"
    >
      <!-- ── Edit mode ──────────────────────────────────────────────── -->
      <template v-if="isEditing">
        <div class="card p-6 md:col-span-2 space-y-5">
          <h3 class="font-semibold">Edit Change Request</h3>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Title -->
            <div class="md:col-span-2">
              <label class="field-label"
                >Title <span class="text-danger">*</span></label
              >
              <input
                v-model="editForm.title"
                class="input mt-1"
                maxlength="500"
              />
            </div>

            <!-- Priority -->
            <div>
              <label class="field-label">Priority</label>
              <select v-model="editForm.priority" class="input mt-1">
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <!-- Risk -->
            <div>
              <label class="field-label">Risk Level</label>
              <select v-model="editForm.riskLevel" class="input mt-1">
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <!-- Approval Type -->
            <div>
              <label class="field-label">Approval Type</label>
              <select v-model="editForm.approvalType" class="input mt-1">
                <option value="LINEAR">Linear</option>
                <option value="NON_LINEAR">Non Linear</option>
              </select>
            </div>

            <!-- Category -->
            <div>
              <label class="field-label">Category</label>
              <input
                v-model="editForm.category"
                class="input mt-1"
                maxlength="255"
              />
            </div>

            <!-- Scheduled Start -->
            <div>
              <label class="field-label">Scheduled Start</label>
              <input
                v-model="editForm.scheduledStart"
                type="datetime-local"
                class="input mt-1"
              />
            </div>

            <!-- Scheduled End -->
            <div>
              <label class="field-label">Scheduled End</label>
              <input
                v-model="editForm.scheduledEnd"
                type="datetime-local"
                class="input mt-1"
              />
            </div>

            <!-- Affected Systems -->
            <div class="md:col-span-2">
              <label class="field-label"
                >Affected Systems (comma separated)</label
              >
              <input
                v-model="editForm.affectedSystemsInput"
                class="input mt-1"
              />
            </div>

            <!-- Description -->
            <div class="md:col-span-2">
              <label class="field-label">Description</label>
              <EditorContent
                :editor="editEditor"
                class="input mt-1 min-h-36 p-3"
              />
            </div>
          </div>

          <!-- Custom fields in edit mode -->
          <template v-if="fieldDefinitions.length">
            <hr class="border-outline-variant/40 dark:border-border-dark" />
            <h4 class="font-medium text-sm">Custom Fields</h4>
            <div class="space-y-3">
              <div
                v-for="def in fieldDefinitions"
                :key="def.id"
                class="grid grid-cols-[200px_1fr] gap-3 items-center"
              >
                <label class="text-sm font-medium">
                  {{ def.label }}
                  <span v-if="def.isRequired" class="text-danger ml-0.5"
                    >*</span
                  >
                </label>
                <select
                  v-if="def.fieldType === 'DROPDOWN'"
                  v-model="localFieldValues[def.id]"
                  class="input"
                >
                  <option value="">&mdash; select &mdash;</option>
                  <option v-for="opt in def.options" :key="opt" :value="opt">
                    {{ opt }}
                  </option>
                </select>
                <input
                  v-else-if="def.fieldType === 'CHECKBOX'"
                  type="checkbox"
                  class="h-4 w-4 accent-primary"
                  :checked="localFieldValues[def.id] === 'true'"
                  @change="
                    localFieldValues[def.id] = (
                      $event.target as HTMLInputElement
                    ).checked
                      ? 'true'
                      : 'false'
                  "
                />
                <input
                  v-else
                  :type="
                    def.fieldType === 'NUMBER'
                      ? 'number'
                      : def.fieldType === 'DATE'
                        ? 'date'
                        : 'text'
                  "
                  v-model="localFieldValues[def.id]"
                  class="input"
                />
              </div>
            </div>
          </template>
        </div>
      </template>

      <!-- ── Read-only view ─────────────────────────────────────────── -->
      <template v-else>
        <div class="card p-5 md:col-span-2 shadow-card-hover">
          <h2 class="font-semibold mb-2">Description</h2>
          <div
            v-if="changeRequest.description"
            class="text-sm text-gray-700 dark:text-gray-300 prose dark:prose-invert max-w-none"
            v-html="changeRequest.description"
          />
          <p v-else class="text-sm text-gray-700 dark:text-gray-300">
            No description.
          </p>
        </div>

        <div class="card p-5">
          <h3 class="font-semibold mb-3">Details</h3>
          <dl class="space-y-1.5 text-sm">
            <div class="flex gap-2">
              <dt class="text-muted w-32 shrink-0">Priority</dt>
              <dd class="font-medium">{{ changeRequest.priority }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="text-muted w-32 shrink-0">Risk Level</dt>
              <dd class="font-medium">{{ changeRequest.riskLevel }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="text-muted w-32 shrink-0">Approval Type</dt>
              <dd class="font-medium">{{ changeRequest.approvalType }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="text-muted w-32 shrink-0">Category</dt>
              <dd class="font-medium">{{ changeRequest.category ?? "—" }}</dd>
            </div>
          </dl>
        </div>

        <div class="card p-5">
          <h3 class="font-semibold mb-3">Scheduling</h3>
          <dl class="space-y-1.5 text-sm">
            <div class="flex gap-2">
              <dt class="text-muted w-32 shrink-0">Start</dt>
              <dd>{{ fmt(changeRequest.scheduledStart) }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="text-muted w-32 shrink-0">End</dt>
              <dd>{{ fmt(changeRequest.scheduledEnd) }}</dd>
            </div>
            <div class="flex gap-2">
              <dt class="text-muted w-32 shrink-0">SLA Deadline</dt>
              <dd>{{ fmt(changeRequest.slaDeadline) }}</dd>
            </div>
          </dl>
        </div>

        <div class="card p-5">
          <h3 class="font-semibold mb-2">Impact</h3>
          <ul class="list-disc ml-5 text-sm text-gray-700 dark:text-gray-300">
            <li v-for="system in changeRequest.affectedSystems" :key="system">
              {{ system }}
            </li>
            <li v-if="!changeRequest.affectedSystems.length">
              No affected systems listed.
            </li>
          </ul>
        </div>

        <!-- Custom fields read-only -->
        <div v-if="fieldDefinitions.length" class="card p-5 md:col-span-2">
          <h3 class="font-semibold mb-3">Custom Fields</h3>
          <dl class="space-y-2">
            <div
              v-for="def in fieldDefinitions"
              :key="def.id"
              class="grid grid-cols-[200px_1fr] gap-2 text-sm"
            >
              <dt class="text-muted font-medium">{{ def.label }}</dt>
              <dd>
                <template v-if="localFieldValues[def.id] === 'true'"
                  >Yes</template
                >
                <template v-else-if="localFieldValues[def.id] === 'false'"
                  >No</template
                >
                <template v-else>{{
                  localFieldValues[def.id] || "—"
                }}</template>
              </dd>
            </div>
          </dl>
        </div>

        <!-- Workflow actions -->
        <div class="card p-5 md:col-span-2 flex flex-wrap gap-2">
          <button
            class="btn-primary btn-md"
            :disabled="changeRequest.status !== 'DRAFT'"
            @click="submitCr"
          >
            Submit
          </button>
          <button
            class="btn-ghost btn-md"
            :disabled="changeRequest.status === 'CANCELLED'"
            @click="cancelCr"
          >
            Cancel
          </button>
          <button
            class="btn-ghost btn-md"
            :disabled="changeRequest.status !== 'PENDING_APPROVAL'"
            @click="approveCr"
          >
            Approve
          </button>
          <button
            class="btn-ghost btn-md"
            :disabled="changeRequest.status !== 'PENDING_APPROVAL'"
            @click="showReject = true"
          >
            Reject
          </button>
        </div>
      </template>

      <div class="card p-5 md:col-span-2">
        <h3 class="font-semibold mb-3">Attachments</h3>
        <div
          class="border-2 border-dashed border-border dark:border-border-dark rounded-lg p-6 text-center"
          @dragover.prevent
          @drop.prevent="onDropUpload"
        >
          <p class="text-sm text-muted">
            Drag and drop files here, or choose a file.
          </p>
          <input
            ref="fileInput"
            class="hidden"
            type="file"
            accept=".png,.jpg,.jpeg,.docx,.xlsx,.pdf"
            @change="onSelectUpload"
          />
          <button
            class="btn-ghost btn-md mt-3"
            :disabled="isUploading"
            @click="fileInput?.click()"
          >
            {{ isUploading ? "Uploading…" : "Select File" }}
          </button>
          <p v-if="uploadError" class="mt-2 text-xs text-danger">
            {{ uploadError }}
          </p>
        </div>

        <div class="mt-4 space-y-2">
          <div
            v-for="file in attachments"
            :key="file.id"
            class="border border-border dark:border-border-dark rounded-lg p-3 flex items-center justify-between"
          >
            <div>
              <p class="text-sm font-semibold">{{ file.fileName }}</p>
              <p class="text-xs text-muted">
                {{ formatSize(file.sizeBytes) }} •
                {{ file.uploaderName ?? "Unknown" }}
              </p>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-xs text-muted">{{ fmt(file.createdAt) }}</span>
              <button
                class="btn-ghost btn-sm"
                :title="`Download ${file.fileName}`"
                @click="handleDownload(file.id, file.fileName)"
              >
                ↓ Download
              </button>
            </div>
          </div>
          <div v-if="!attachments.length" class="text-sm text-muted">
            No attachments uploaded yet.
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="tab === 'approvers'" class="card p-5 space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="font-semibold">Approvers</h2>
        <button
          class="btn-ghost btn-md"
          @click="showAddApprover = !showAddApprover"
        >
          Add Approver
        </button>
      </div>

      <div v-if="showAddApprover" class="grid grid-cols-1 md:grid-cols-3 gap-2">
        <input
          v-model="newApproverUserId"
          class="input"
          placeholder="User ID"
        />
        <label class="flex items-center gap-2 text-sm"
          ><input v-model="newApproverRequired" type="checkbox" />
          Required</label
        >
        <button class="btn-primary btn-md" @click="addApproverAction">
          Save
        </button>
      </div>

      <div class="space-y-2">
        <div
          v-for="a in approvers"
          :key="a.id"
          class="border border-border dark:border-border-dark rounded-lg p-3 flex items-center justify-between"
        >
          <div>
            <p class="text-sm font-semibold">
              {{ a.userFullName }}
              <span class="text-xs text-muted">({{ a.userEmail }})</span>
            </p>
            <p class="text-xs text-muted">
              Position {{ a.position }} • {{ a.status }} •
              {{ a.isRequired ? "Required" : "Optional" }}
            </p>
          </div>
          <div class="flex gap-2">
            <button
              class="btn-ghost btn-md"
              @click="moveUp(a.id)"
              :disabled="a.position === 1"
            >
              Up
            </button>
            <button
              class="btn-ghost btn-md"
              @click="removeApproverAction(a.id)"
            >
              Remove
            </button>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="tab === 'activity'" class="card p-5">
      <h2 class="font-semibold mb-2">Activity Stream</h2>
      <div v-if="!activity.length" class="text-sm text-muted">
        No activity yet.
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="event in activity"
          :key="event.id"
          class="border border-border dark:border-border-dark rounded-lg p-3"
        >
          <p class="text-sm font-semibold">{{ event.actionType }}</p>
          <p class="text-xs text-muted mt-1">
            {{ event.actorFullName ?? "System" }} • {{ fmt(event.createdAt) }}
          </p>
          <pre
            v-if="event.payload"
            class="text-xs mt-2 p-2 bg-surface dark:bg-surface-dark rounded"
            >{{ JSON.stringify(event.payload, null, 2) }}</pre
          >
        </div>
      </div>
    </section>

    <section v-else class="card p-5 space-y-4">
      <h2 class="font-semibold">Comments</h2>
      <div class="space-y-3">
        <div
          v-for="comment in comments"
          :key="comment.id"
          class="border border-border dark:border-border-dark rounded-lg p-3"
        >
          <p class="text-xs text-muted">
            {{ comment.author?.fullName ?? "Unknown" }} •
            {{ fmt(comment.createdAt) }}
          </p>
          <div
            class="text-sm mt-2 text-gray-800 dark:text-gray-200"
            v-html="comment.body"
          />
        </div>
        <div v-if="!comments.length" class="text-sm text-muted">
          No comments yet.
        </div>
      </div>

      <div class="space-y-2">
        <textarea
          v-model="newComment"
          class="input"
          rows="4"
          placeholder="Add a comment. Mention users with @user@example.com"
        />
        <div class="flex justify-end">
          <button class="btn-primary btn-md" @click="postCommentAction">
            Post Comment
          </button>
        </div>
      </div>
    </section>

    <div
      v-if="showReject"
      class="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50"
    >
      <div class="card p-5 max-w-lg w-full space-y-3">
        <h3 class="font-semibold">Reject Change Request</h3>
        <textarea
          v-model="rejectReason"
          class="input"
          rows="4"
          placeholder="Reason"
        ></textarea>
        <div class="flex justify-end gap-2">
          <button class="btn-ghost btn-md" @click="showReject = false">
            Close
          </button>
          <button class="btn-primary btn-md" @click="rejectCr">
            Confirm Reject
          </button>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="py-16 text-center text-sm text-muted">
    Loading change request…
  </div>
</template>

<script setup lang="ts">
import type {
  ActivityEntry,
  Attachment,
  ChangeRequest,
  ChangeRequestCustomFieldValue,
  Comment,
  CrApprover,
  CustomFieldDefinition,
} from "~/types";
import { format, parseISO } from "date-fns";
import { EditorContent, useEditor } from "@tiptap/vue-3";
import StarterKit from "@tiptap/starter-kit";

definePageMeta({ middleware: "auth" });

const { error: toastError } = useToast();
const api = useApi();

const route = useRoute();
const id = computed(() => String(route.params.id));
const {
  get,
  update,
  submit,
  cancel,
  approve,
  reject,
  listApprovers,
  addApprover,
  removeApprover,
  reorderApprovers,
  listCustomFields,
  saveCustomFields,
  listAttachments,
  uploadAttachment,
  downloadAttachment,
  listActivity,
  listComments,
  postComment,
} = useChangeRequests();

const changeRequest = ref<ChangeRequest | null>(null);
const approvers = ref<CrApprover[]>([]);
const customFields = ref<ChangeRequestCustomFieldValue[]>([]);
const fieldDefinitions = ref<CustomFieldDefinition[]>([]);
const localFieldValues = ref<Record<string, string>>({});
const attachments = ref<Attachment[]>([]);
const activity = ref<ActivityEntry[]>([]);
const comments = ref<Comment[]>([]);
const tab = ref<"details" | "approvers" | "activity" | "comments">("details");
const newComment = ref("");

const showAddApprover = ref(false);
const newApproverUserId = ref("");
const newApproverRequired = ref(true);
const fileInput = ref<HTMLInputElement | null>(null);

const showReject = ref(false);
const rejectReason = ref("");

// ── Edit mode ───────────────────────────────────────────────────────────────
const isEditing = ref(false);
const isSaving = ref(false);

const editForm = reactive({
  title: "",
  priority: "",
  riskLevel: "",
  approvalType: "",
  category: "",
  scheduledStart: "",
  scheduledEnd: "",
  affectedSystemsInput: "",
});

const editEditor = useEditor({
  extensions: [StarterKit],
  editorProps: {
    attributes: {
      class:
        "prose dark:prose-invert max-w-none focus:outline-none min-h-[8rem]",
    },
  },
});

function toDatetimeLocal(iso: string | null): string {
  if (!iso) return "";
  // datetime-local input expects 'YYYY-MM-DDTHH:mm'
  return iso.substring(0, 16);
}

function enterEditMode() {
  if (!changeRequest.value) return;
  const cr = changeRequest.value;
  editForm.title = cr.title;
  editForm.priority = cr.priority;
  editForm.riskLevel = cr.riskLevel;
  editForm.approvalType = cr.approvalType;
  editForm.category = cr.category ?? "";
  editForm.scheduledStart = toDatetimeLocal(cr.scheduledStart);
  editForm.scheduledEnd = toDatetimeLocal(cr.scheduledEnd);
  editForm.affectedSystemsInput = cr.affectedSystems.join(", ");
  editEditor.value?.commands.setContent(cr.description ?? "");
  isEditing.value = true;
}

function cancelEdit() {
  isEditing.value = false;
}

async function saveEditAction() {
  if (!changeRequest.value || !editForm.title.trim()) {
    toastError("Title is required.");
    return;
  }
  isSaving.value = true;
  try {
    const payload: Record<string, unknown> = {
      title: editForm.title.trim(),
      description: editEditor.value?.getHTML() ?? null,
      priority: editForm.priority,
      riskLevel: editForm.riskLevel,
      approvalType: editForm.approvalType,
      category: editForm.category.trim() || null,
      scheduledStart: editForm.scheduledStart
        ? new Date(editForm.scheduledStart).toISOString()
        : null,
      scheduledEnd: editForm.scheduledEnd
        ? new Date(editForm.scheduledEnd).toISOString()
        : null,
      affectedSystems: editForm.affectedSystemsInput
        .split(",")
        .map((v) => v.trim())
        .filter(Boolean),
    };
    changeRequest.value = await update(id.value, payload);
    await saveCustomFieldsAction();
    isEditing.value = false;
  } catch (err: unknown) {
    toastError(
      (err as { data?: { detail?: string } })?.data?.detail ??
        "Failed to save changes.",
    );
  } finally {
    isSaving.value = false;
  }
}

async function loadAll() {
  const [
    cr,
    approverList,
    savedFields,
    definitions,
    attachmentList,
    activityList,
    commentList,
  ] = await Promise.all([
    get(id.value),
    listApprovers(id.value),
    listCustomFields(id.value),
    api<CustomFieldDefinition[]>("/api/v1/admin/custom-fields"),
    listAttachments(id.value),
    listActivity(id.value),
    listComments(id.value),
  ]);
  changeRequest.value = cr;
  approvers.value = approverList;
  customFields.value = savedFields;
  fieldDefinitions.value = definitions;
  attachments.value = attachmentList;
  activity.value = activityList;
  comments.value = commentList;
  // Build the editable map from saved values; fall back to empty string per definition.
  const valueMap: Record<string, string> = {};
  for (const def of definitions) {
    const saved = savedFields.find((f) => f.fieldId === def.id);
    valueMap[def.id] = saved?.value ?? "";
  }
  localFieldValues.value = valueMap;
}

function fmt(value: string | null) {
  return value ? format(parseISO(value), "MMM d, yyyy HH:mm") : "—";
}

async function saveCustomFieldsAction() {
  if (!fieldDefinitions.value.length) {
    return;
  }
  const fields = fieldDefinitions.value.map((def) => ({
    fieldId: def.id,
    // Convert empty string to null so the backend stores absence of value cleanly.
    value: localFieldValues.value[def.id] || null,
  }));
  customFields.value = await saveCustomFields(id.value, fields);
  activity.value = await listActivity(id.value);
}

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

const uploadError = ref<string | null>(null);
const isUploading = ref(false);

function isFileTypeAllowed(file: File): boolean {
  const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
  return ALLOWED_EXTENSIONS.has(ext) && ALLOWED_MIME_TYPES.has(file.type);
}

async function uploadSelected(file: File | null) {
  if (!file) {
    return;
  }
  if (!isFileTypeAllowed(file)) {
    const msg = "Only PNG, JPG, DOCX, XLSX, and PDF files are permitted.";
    uploadError.value = msg;
    toastError(msg);
    return;
  }
  uploadError.value = null;
  isUploading.value = true;
  try {
    await uploadAttachment(id.value, file);
    attachments.value = await listAttachments(id.value);
    activity.value = await listActivity(id.value);
  } catch (err: unknown) {
    const message =
      (err as { data?: { detail?: string } })?.data?.detail ??
      "Upload failed. Please try again.";
    uploadError.value = message;
    toastError(message);
  } finally {
    isUploading.value = false;
  }
}

async function handleDownload(attachmentId: string, fileName: string) {
  try {
    await downloadAttachment(id.value, attachmentId, fileName);
  } catch {
    toastError("Download failed. Please try again.");
  }
}

function onSelectUpload(event: Event) {
  const target = event.target as HTMLInputElement;
  uploadSelected(target.files?.item(0) ?? null);
  target.value = "";
}

function onDropUpload(event: DragEvent) {
  uploadSelected(event.dataTransfer?.files?.item(0) ?? null);
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

async function submitCr() {
  changeRequest.value = await submit(id.value);
  activity.value = await listActivity(id.value);
}

async function cancelCr() {
  await cancel(id.value);
  changeRequest.value = await get(id.value);
  activity.value = await listActivity(id.value);
}

async function approveCr() {
  changeRequest.value = await approve(id.value);
  approvers.value = await listApprovers(id.value);
  activity.value = await listActivity(id.value);
}

async function rejectCr() {
  if (!rejectReason.value.trim()) {
    return;
  }
  changeRequest.value = await reject(id.value, rejectReason.value);
  showReject.value = false;
  rejectReason.value = "";
  approvers.value = await listApprovers(id.value);
  activity.value = await listActivity(id.value);
}

async function addApproverAction() {
  if (!newApproverUserId.value.trim()) {
    return;
  }
  await addApprover(id.value, {
    userId: newApproverUserId.value.trim(),
    isRequired: newApproverRequired.value,
  });
  approvers.value = await listApprovers(id.value);
  activity.value = await listActivity(id.value);
  showAddApprover.value = false;
  newApproverUserId.value = "";
}

async function removeApproverAction(approverId: string) {
  await removeApprover(id.value, approverId);
  approvers.value = await listApprovers(id.value);
  activity.value = await listActivity(id.value);
}

async function moveUp(approverId: string) {
  const order = [...approvers.value]
    .sort((a, b) => a.position - b.position)
    .map((a) => a.id);
  const index = order.indexOf(approverId);
  if (index <= 0) {
    return;
  }
  const previous = order[index - 1];
  order[index - 1] = approverId;
  order[index] = previous;
  approvers.value = await reorderApprovers(id.value, order);
  activity.value = await listActivity(id.value);
}

async function postCommentAction() {
  const body = newComment.value.trim();
  if (!body) {
    return;
  }
  await postComment(id.value, body);
  newComment.value = "";
  comments.value = await listComments(id.value);
  activity.value = await listActivity(id.value);
}

onMounted(loadAll);

onBeforeUnmount(() => {
  editEditor.value?.destroy();
});
</script>
