<template>
  <div v-if="changeRequest" class="space-y-6">
    <div class="flex items-start justify-between">
      <div>
        <p class="text-xs text-muted uppercase tracking-widest mb-1">
          Change Request
        </p>
        <h1
          class="text-3xl font-bold tracking-tight text-gray-900 dark:text-gray-100"
        >
          {{ changeRequest.title }}
        </h1>
        <p class="text-sm text-muted mt-1">
          Created by {{ changeRequest.createdByFullName ?? "Unknown" }}
        </p>
      </div>
      <div class="flex items-center gap-2 flex-wrap">
        <CrStatusBadge :status="changeRequest.status" />
        <CrPriorityBadge :priority="changeRequest.priority" />
        <button
          v-if="changeRequest.status === 'DRAFT' && !isEditing"
          class="btn-ghost btn-md"
          @click="enterEditMode"
        >
          Edit
        </button>
      </div>
    </div>

    <div class="card p-4">
      <div
        role="tablist"
        aria-label="Change request sections"
        class="flex items-center gap-1 p-1 bg-surface-container-low dark:bg-slate-800 rounded-xl"
      >
        <button
          v-for="crTab in crTabs"
          :key="crTab.key"
          role="tab"
          :aria-selected="tab === crTab.key"
          :tabindex="tab === crTab.key ? 0 : -1"
          type="button"
          @click="tab = crTab.key"
          @keydown="onTabKeyDown($event, crTab.key)"
          class="relative flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-all duration-150 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary"
          :class="
            tab === crTab.key
              ? 'bg-white dark:bg-slate-700 text-on-surface dark:text-gray-100 shadow-sm'
              : 'text-muted hover:text-on-surface hover:bg-white/50 dark:hover:text-gray-300 dark:hover:bg-slate-700/50'
          "
        >
          {{ crTab.label }}
          <span
            v-if="crTab.count !== undefined"
            class="flex h-5 min-w-5 items-center justify-center rounded-full px-1 text-[10px] font-semibold"
            :class="
              tab === crTab.key
                ? 'bg-primary/15 text-primary'
                : 'bg-outline-variant/40 text-muted'
            "
            aria-hidden="true"
          >
            {{ crTab.count }}
          </span>
        </button>
      </div>
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
              <p class="field-label">
                Title <span class="text-danger">*</span>
              </p>
              <input
                v-model="editForm.title"
                class="input mt-1"
                maxlength="500"
              />
            </div>

            <!-- Priority -->
            <div>
              <p class="field-label">Priority</p>
              <select v-model="editForm.priority" class="input mt-1">
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <!-- Risk -->
            <div>
              <p class="field-label">Risk Level</p>
              <select v-model="editForm.riskLevel" class="input mt-1">
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="CRITICAL">Critical</option>
              </select>
            </div>

            <!-- Approval Type -->
            <div>
              <p class="field-label">Approval Type</p>
              <select v-model="editForm.approvalType" class="input mt-1">
                <option value="LINEAR">Linear</option>
                <option value="NON_LINEAR">Non-Linear</option>
              </select>
            </div>

            <!-- Category -->
            <div>
              <p class="field-label">Category</p>
              <input
                v-model="editForm.category"
                class="input mt-1"
                maxlength="255"
              />
            </div>

            <!-- Scheduled Start -->
            <div>
              <p class="field-label">Scheduled Start</p>
              <div class="mt-1 grid grid-cols-2 gap-2">
                <FlatPickr
                  v-model="editForm.scheduledStartDate"
                  :config="datePickerConfig"
                  class="input"
                />
                <FlatPickr
                  v-model="editForm.scheduledStartTime"
                  :config="timePickerConfig"
                  class="input"
                />
              </div>
            </div>

            <!-- Scheduled End -->
            <div>
              <p class="field-label">Scheduled End</p>
              <div class="mt-1 grid grid-cols-2 gap-2">
                <FlatPickr
                  v-model="editForm.scheduledEndDate"
                  :config="endDatePickerConfig"
                  class="input"
                />
                <FlatPickr
                  v-model="editForm.scheduledEndTime"
                  :config="timePickerConfig"
                  class="input"
                />
              </div>
            </div>

            <!-- Affected Systems -->
            <div class="md:col-span-2">
              <p class="field-label">Affected Systems</p>
              <div
                class="input mt-1 flex flex-wrap gap-1 min-h-[2.5rem] cursor-text"
                @click="editAffectedSystemsInputRef?.focus()"
              >
                <span
                  v-for="sys in editForm.affectedSystems"
                  :key="sys"
                  class="inline-flex items-center gap-1 rounded bg-primary/10 text-primary text-xs px-2 py-0.5 shrink-0"
                >
                  {{ sys }}
                  <button
                    type="button"
                    class="hover:text-danger leading-none"
                    :aria-label="`Remove ${sys}`"
                    @click.stop="removeEditAffectedSystem(sys)"
                  >
                    ×
                  </button>
                </span>
                <input
                  ref="editAffectedSystemsInputRef"
                  v-model="editAffectedSystemsTagInput"
                  class="flex-1 min-w-[10rem] bg-transparent outline-none text-sm"
                  placeholder="Add system and press Enter…"
                  @keydown.enter.prevent="addEditAffectedSystem"
                  @keydown="onEditAffectedSystemsKeydown"
                  @keydown.backspace="onEditAffectedSystemsBackspace"
                />
              </div>
              <p class="field-hint">Press Enter or comma to add each system.</p>
            </div>

            <!-- Description -->
            <div class="md:col-span-2">
              <p class="field-label">Description</p>
              <div class="rich-editor-shell">
                <SharedRichTextToolbar :editor="editEditor" />
                <EditorContent
                  :editor="editEditor"
                  class="rich-editor-content"
                />
              </div>
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
                <label class="text-sm font-medium" :for="`cf-${def.id}`">
                  {{ def.label }}
                  <span v-if="def.isRequired" class="text-danger ml-0.5"
                    >*</span
                  >
                </label>
                <select
                  v-if="def.fieldType === 'DROPDOWN'"
                  :id="`cf-${def.id}`"
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
                  :id="`cf-${def.id}`"
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
                  :id="`cf-${def.id}`"
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
            v-if="canSeeApprovalActions"
            class="btn-ghost btn-md"
            :disabled="!canCastVote"
            @click="approveCr"
          >
            Approve
          </button>
          <button
            v-if="canSeeApprovalActions"
            class="btn-ghost btn-md"
            :disabled="!canCastVote"
            @click="showReject = true"
          >
            Reject
          </button>
        </div>
      </template>

      <div class="card p-5 md:col-span-2">
        <h3 class="font-semibold mb-3">Attachments</h3>
        <template v-if="changeRequest.status === 'DRAFT' && isEditing">
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
        </template>

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

      <div class="card p-5 md:col-span-2" v-if="recordedVotes.length">
        <h3 class="font-semibold mb-3">Recorded Votes</h3>
        <div class="space-y-2 text-sm">
          <div
            v-for="approver in recordedVotes"
            :key="approver.id"
            class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-outline-variant/50 bg-surface-container-low px-3 py-2 dark:border-slate-600 dark:bg-slate-800/70"
          >
            <div>
              <p class="font-medium text-on-surface dark:text-gray-100">
                {{ approver.userFullName }}
              </p>
              <p class="text-xs text-muted">{{ approver.userEmail }}</p>
            </div>
            <div class="text-right">
              <p class="font-medium">{{ formatEnumLabel(approver.status) }}</p>
              <p v-if="approver.decidedAt" class="text-xs text-muted">
                {{ fmt(approver.decidedAt) }}
              </p>
              <p v-if="approver.rejectionReason" class="text-xs text-danger">
                {{ approver.rejectionReason }}
              </p>
            </div>
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
          class="border border-border dark:border-border-dark rounded-xl p-4 bg-surface-container-low/50 dark:bg-slate-800/70"
        >
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p
                class="text-sm font-semibold text-on-surface dark:text-gray-100"
              >
                {{ formatActivityAction(event.actionType) }}
              </p>
              <p class="text-xs text-muted mt-1">
                {{ event.actorFullName ?? "System" }} •
                {{ fmt(event.createdAt) }}
              </p>
            </div>
            <span class="badge badge-draft">{{
              formatActivityBadge(event.actionType)
            }}</span>
          </div>
          <p
            v-if="activitySummary(event)"
            class="mt-3 text-sm text-gray-700 dark:text-gray-300"
          >
            {{ activitySummary(event) }}
          </p>
          <dl
            v-if="activityFields(event).length"
            class="mt-3 grid gap-2 rounded-lg bg-white/80 p-3 text-xs dark:bg-slate-900/50 sm:grid-cols-2"
          >
            <div v-for="field in activityFields(event)" :key="field.label">
              <dt class="font-semibold uppercase tracking-[0.08em] text-muted">
                {{ field.label }}
              </dt>
              <dd class="mt-1 text-sm text-on-surface dark:text-gray-200">
                {{ field.value }}
              </dd>
            </div>
          </dl>
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

    <!-- Reject modal using AppModal for proper focus management -->
    <SharedAppModal
      :open="showReject"
      title="Reject Change Request"
      @close="showReject = false"
    >
      <div class="space-y-3">
        <p class="text-sm text-muted">
          Provide a reason for rejecting this change request.
        </p>
        <div>
          <label for="reject-reason" class="field-label"
            >Reason <span class="text-danger">*</span></label
          >
          <textarea
            id="reject-reason"
            v-model="rejectReason"
            class="input mt-1"
            rows="4"
            placeholder="Describe why this change request is being rejected…"
          />
        </div>
      </div>
      <template #footer>
        <button class="btn-ghost btn-md" @click="showReject = false">
          Cancel
        </button>
        <button class="btn-primary btn-md" @click="rejectCr">
          Confirm Reject
        </button>
      </template>
    </SharedAppModal>

    <!-- Sticky save bar (visible when editing) -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="translate-y-full opacity-0"
      enter-to-class="translate-y-0 opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="translate-y-0 opacity-100"
      leave-to-class="translate-y-full opacity-0"
    >
      <div
        v-if="isEditing"
        class="fixed bottom-0 left-0 md:left-56 right-0 z-20 bg-white/95 dark:bg-slate-900/95 backdrop-blur-sm border-t border-outline-variant/50 dark:border-border-dark px-6 py-3 flex items-center justify-between gap-3 shadow-[0_-2px_8px_rgba(0,35,111,0.06)]"
      >
        <p class="text-sm text-muted hidden sm:block">
          You have unsaved changes
        </p>
        <div class="flex items-center gap-2">
          <button class="btn-ghost btn-md" @click="cancelEdit">Discard</button>
          <button
            class="btn-primary btn-md"
            :disabled="isSaving"
            @click="saveEditAction"
          >
            {{ isSaving ? "Saving…" : "Save Changes" }}
          </button>
        </div>
      </div>
    </Transition>
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
import FlatPickr from "vue-flatpickr-component";

definePageMeta({ middleware: "auth" });

const changeRequest = ref<ChangeRequest | null>(null);

useHead(
  computed(() => ({
    title: changeRequest.value
      ? `${changeRequest.value.title} — Audita`
      : "Change Request — Audita",
  })),
);

const { error: toastError } = useToast();
const auth = useAuthStore();
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

const approvers = ref<CrApprover[]>([]);
const customFields = ref<ChangeRequestCustomFieldValue[]>([]);
const fieldDefinitions = ref<CustomFieldDefinition[]>([]);
const localFieldValues = ref<Record<string, string>>({});
const attachments = ref<Attachment[]>([]);
const activity = ref<ActivityEntry[]>([]);
const comments = ref<Comment[]>([]);
const tab = ref<string>("details");
const newComment = ref("");

const sortedApprovers = computed(() =>
  [...approvers.value].sort((left, right) => left.position - right.position),
);
const recordedVotes = computed(() =>
  sortedApprovers.value.filter((approver) => approver.status !== "PENDING"),
);
const canSeeApprovalActions = computed(() => {
  if (
    !changeRequest.value ||
    changeRequest.value.status !== "PENDING_APPROVAL"
  ) {
    return false;
  }
  if (auth.role === "Auditor") {
    return false;
  }
  if (auth.isSuperAdmin || auth.role === "Admin") {
    return true;
  }
  return approvers.value.some((approver) => approver.userId === auth.userId);
});
const canCastVote = computed(() => {
  if (
    !changeRequest.value ||
    changeRequest.value.status !== "PENDING_APPROVAL"
  ) {
    return false;
  }
  if (auth.role === "Auditor") {
    return false;
  }
  if (auth.isSuperAdmin || auth.role === "Admin") {
    return true;
  }

  const selfApprover = sortedApprovers.value.find(
    (approver) =>
      approver.userId === auth.userId && approver.status === "PENDING",
  );
  if (!selfApprover) {
    return false;
  }
  if (changeRequest.value.approvalType !== "LINEAR") {
    return true;
  }

  return (
    sortedApprovers.value.find((approver) => approver.status === "PENDING")
      ?.id === selfApprover.id
  );
});

// ── Tab list with counts ────────────────────────────────────────────────────
const crTabs = computed(() => [
  { key: "details", label: "Details" },
  { key: "approvers", label: "Approvers", count: approvers.value.length },
  { key: "activity", label: "Activity", count: activity.value.length },
  { key: "comments", label: "Comments", count: comments.value.length },
]);

const tabKeys = ["details", "approvers", "activity", "comments"];

function onTabKeyDown(e: KeyboardEvent, key: string) {
  const idx = tabKeys.indexOf(key);
  if (e.key === "ArrowRight") {
    e.preventDefault();
    const next = (idx + 1) % tabKeys.length;
    tab.value = tabKeys[next];
  } else if (e.key === "ArrowLeft") {
    e.preventDefault();
    const prev = (idx - 1 + tabKeys.length) % tabKeys.length;
    tab.value = tabKeys[prev];
  } else if (e.key === "Home") {
    e.preventDefault();
    tab.value = tabKeys[0];
  } else if (e.key === "End") {
    e.preventDefault();
    tab.value = tabKeys[tabKeys.length - 1];
  }
}

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
  scheduledStartDate: "",
  scheduledStartTime: "",
  scheduledEndDate: "",
  scheduledEndTime: "",
  affectedSystems: [] as string[],
});

// ── Edit affected-systems tag input ────────────────────────────────────────
const editAffectedSystemsInputRef = ref<HTMLInputElement | null>(null);
const editAffectedSystemsTagInput = ref("");

function addEditAffectedSystem() {
  const val = editAffectedSystemsTagInput.value.replaceAll(",", "").trim();
  if (val && !editForm.affectedSystems.includes(val)) {
    editForm.affectedSystems.push(val);
  }
  editAffectedSystemsTagInput.value = "";
}

function removeEditAffectedSystem(sys: string) {
  editForm.affectedSystems = editForm.affectedSystems.filter((s) => s !== sys);
}

function onEditAffectedSystemsKeydown(e: KeyboardEvent) {
  if (e.key === ",") {
    e.preventDefault();
    addEditAffectedSystem();
  }
}

function onEditAffectedSystemsBackspace() {
  if (!editAffectedSystemsTagInput.value && editForm.affectedSystems.length) {
    editForm.affectedSystems.pop();
  }
}

const editEditor = useEditor({
  extensions: [StarterKit],
  editorProps: {
    attributes: {
      class:
        "prose dark:prose-invert max-w-none focus:outline-none min-h-[8rem]",
    },
  },
});

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
  minDate: editForm.scheduledStartDate || undefined,
}));

function combineParts(dateStr: string, timeStr: string): Date | null {
  if (!dateStr) return null;
  const [year, month, day] = dateStr.split("-").map(Number);
  const [hours, minutes] = timeStr ? timeStr.split(":").map(Number) : [0, 0];
  return new Date(year, month - 1, day, hours, minutes, 0, 0);
}

function onEditStartChange() {
  const start = combineParts(
    editForm.scheduledStartDate,
    editForm.scheduledStartTime,
  );
  const end = combineParts(
    editForm.scheduledEndDate,
    editForm.scheduledEndTime,
  );
  if (start && end && end <= start) {
    editForm.scheduledEndDate = "";
    editForm.scheduledEndTime = "";
  }
}

function enterEditMode() {
  if (!changeRequest.value) return;
  const cr = changeRequest.value;
  editForm.title = cr.title;
  editForm.priority = cr.priority;
  editForm.riskLevel = cr.riskLevel;
  editForm.approvalType = cr.approvalType;
  editForm.category = cr.category ?? "";
  if (cr.scheduledStart) {
    const s = new Date(cr.scheduledStart);
    editForm.scheduledStartDate = `${s.getFullYear()}-${String(s.getMonth() + 1).padStart(2, "0")}-${String(s.getDate()).padStart(2, "0")}`;
    editForm.scheduledStartTime = `${String(s.getHours()).padStart(2, "0")}:${String(s.getMinutes()).padStart(2, "0")}`;
  } else {
    editForm.scheduledStartDate = "";
    editForm.scheduledStartTime = "";
  }
  if (cr.scheduledEnd) {
    const e = new Date(cr.scheduledEnd);
    editForm.scheduledEndDate = `${e.getFullYear()}-${String(e.getMonth() + 1).padStart(2, "0")}-${String(e.getDate()).padStart(2, "0")}`;
    editForm.scheduledEndTime = `${String(e.getHours()).padStart(2, "0")}:${String(e.getMinutes()).padStart(2, "0")}`;
  } else {
    editForm.scheduledEndDate = "";
    editForm.scheduledEndTime = "";
  }
  editForm.affectedSystems = cr.affectedSystems.slice();
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
      scheduledStart:
        combineParts(
          editForm.scheduledStartDate,
          editForm.scheduledStartTime,
        )?.toISOString() ?? null,
      scheduledEnd:
        combineParts(
          editForm.scheduledEndDate,
          editForm.scheduledEndTime,
        )?.toISOString() ?? null,
      affectedSystems: editForm.affectedSystems,
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
  try {
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
    const valueMap: Record<string, string> = {};
    for (const def of definitions) {
      const saved = savedFields.find((field) => field.fieldId === def.id);
      valueMap[def.id] = saved?.value ?? "";
    }
    localFieldValues.value = valueMap;
  } catch (error: unknown) {
    toastError(
      extractErrorMessage(error, "Failed to load change request details."),
    );
  }
}

function fmt(value: string | null) {
  return value ? format(parseISO(value), "MMM d, yyyy HH:mm") : "—";
}

function formatEnumLabel(value: string | null | undefined) {
  if (!value) {
    return "—";
  }

  const normalized = value.replace(/^CR_/, "CHANGE_REQUEST_");
  return normalized
    .split("_")
    .filter(Boolean)
    .map((segment) => {
      if (segment === "CHANGE") {
        return "Change";
      }
      if (segment === "REQUEST") {
        return "Request";
      }
      return segment.charAt(0) + segment.slice(1).toLowerCase();
    })
    .join(" ");
}

function formatActivityAction(actionType: string) {
  return formatEnumLabel(actionType);
}

function formatActivityBadge(actionType: string) {
  return actionType.startsWith("CR_") ? "Workflow" : "Event";
}

function formatActivityValue(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "—";
  }
  if (typeof value === "string") {
    return value.includes("_") ? formatEnumLabel(value) : value;
  }
  return String(value);
}

function activitySummary(event: ActivityEntry) {
  const reason = event.payload?.reason;
  if (typeof reason === "string" && reason.trim()) {
    return reason.trim();
  }
  const mentions = event.payload?.mentions;
  if (typeof mentions === "number" && mentions > 0) {
    return `${mentions} mention${mentions === 1 ? "" : "s"} included in this comment.`;
  }
  return null;
}

function activityFields(event: ActivityEntry) {
  if (!event.payload) {
    return [];
  }
  return Object.entries(event.payload)
    .filter(
      ([key, value]) =>
        !key.endsWith("Id") &&
        key !== "reason" &&
        value !== null &&
        value !== undefined,
    )
    .map(([key, value]) => ({
      label: formatEnumLabel(key),
      value: formatActivityValue(value),
    }));
}

function extractErrorMessage(error: unknown, fallback: string) {
  return (error as { data?: { detail?: string } })?.data?.detail ?? fallback;
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
  if (!isEditing.value || changeRequest.value?.status !== "DRAFT") {
    const msg =
      "Enter Edit mode to upload attachments for this change request.";
    uploadError.value = msg;
    toastError(msg);
    return;
  }
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
  try {
    changeRequest.value = await submit(id.value);
    activity.value = await listActivity(id.value);
  } catch (error: unknown) {
    toastError(
      extractErrorMessage(error, "Unable to submit this change request."),
    );
  }
}

async function cancelCr() {
  try {
    await cancel(id.value);
    changeRequest.value = await get(id.value);
    activity.value = await listActivity(id.value);
  } catch (error: unknown) {
    toastError(
      extractErrorMessage(error, "Unable to cancel this change request."),
    );
  }
}

async function approveCr() {
  try {
    changeRequest.value = await approve(id.value);
    approvers.value = await listApprovers(id.value);
    activity.value = await listActivity(id.value);
  } catch (error: unknown) {
    toastError(
      extractErrorMessage(error, "Unable to approve this change request."),
    );
  }
}

async function rejectCr() {
  if (!rejectReason.value.trim()) {
    toastError("A rejection reason is required.");
    return;
  }
  try {
    changeRequest.value = await reject(id.value, rejectReason.value);
    showReject.value = false;
    rejectReason.value = "";
    approvers.value = await listApprovers(id.value);
    activity.value = await listActivity(id.value);
  } catch (error: unknown) {
    toastError(
      extractErrorMessage(error, "Unable to reject this change request."),
    );
  }
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
  try {
    await postComment(id.value, body);
    newComment.value = "";
    comments.value = await listComments(id.value);
    activity.value = await listActivity(id.value);
  } catch (error: unknown) {
    toastError(extractErrorMessage(error, "Unable to post your comment."));
  }
}

onMounted(() => {
  loadAll();
});

watch(
  () => [editForm.scheduledStartDate, editForm.scheduledStartTime],
  () => onEditStartChange(),
);
</script>
