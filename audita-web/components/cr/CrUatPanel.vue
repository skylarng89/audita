<template>
  <div class="space-y-4">
    <div v-if="!isUatAvailable" class="text-center py-8 text-sm text-muted">
      <p class="font-medium">UAT is not available for this request.</p>
      <p class="mt-1">
        UAT requires the request to be approved and using the Delivery Pipeline workflow.
      </p>
    </div>

    <div v-else-if="loading" class="text-center py-8 text-sm text-muted">
      Loading UAT…
    </div>

    <div v-else-if="!uat && !showCreateForm" class="text-center py-8">
      <p class="text-sm text-muted mb-4">
        No UAT has been initiated for this request.
      </p>
      <button
        v-if="canCreateUat"
        class="btn-primary btn-md"
        @click="showCreateForm = true"
      >
        Initiate UAT
      </button>
    </div>

    <div v-else-if="showCreateForm || (uat && !uat.readOnly && isEditing)" class="card p-5 space-y-4">
      <h3 class="font-semibold">{{ uat ? "Edit UAT" : "Initiate UAT" }}</h3>
      <div>
        <p class="field-label">Title <span class="text-danger">*</span></p>
        <input v-model="formTitle" class="input mt-1" maxlength="255" />
      </div>
      <div>
        <p class="field-label">Details</p>
        <div class="rich-editor-shell mt-1">
          <SharedRichTextToolbar :editor="formEditor" />
          <EditorContent :editor="formEditor" class="rich-editor-content min-h-[120px]" />
        </div>
      </div>
      <div class="flex items-center gap-2">
        <button
          class="btn-primary btn-md"
          :disabled="!formTitle.trim() || isSaving"
          @click="saveUat"
        >
          {{ isSaving ? "Saving…" : (uat ? "Update UAT" : "Create UAT") }}
        </button>
        <button class="btn-ghost btn-md" @click="cancelForm">Cancel</button>
      </div>
    </div>

    <template v-else-if="uat">
      <div class="card p-5 space-y-4">
        <div class="flex items-start justify-between">
          <div>
            <h3 class="font-semibold text-lg">{{ uat.title }}</h3>
            <p class="text-xs text-muted mt-1">
              Created by {{ uat.createdByFullName ?? "Unknown" }} •
              {{ formatDateTimeInTenantTimezone(uat.createdAt) }}
            </p>
          </div>
          <div class="flex items-center gap-2">
            <span
              v-if="uat.status === 'PROMOTED'"
              class="rounded-full px-3 py-1 text-xs font-semibold bg-success/15 text-success"
            >
              Promoted to Deployment
            </span>
            <span
              v-else
              class="rounded-full px-3 py-1 text-xs font-semibold"
              :class="statusClass"
            >
              {{ formatEnumLabel(uat.status) }}
            </span>
            <button
              v-if="canManage && !uat.readOnly"
              class="btn-ghost btn-md"
              @click="startEdit"
            >
              Edit
            </button>
          </div>
        </div>

        <div
          v-if="uat.details"
          class="text-sm text-gray-700 dark:text-gray-300 rich-content"
          v-html="normalizeHtml(uat.details)"
        />
      </div>

      <div v-if="!uat.readOnly" class="card p-5 space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="font-semibold">UAT Approvers</h3>
          <button
            v-if="canManage"
            class="btn-ghost btn-md"
            @click="showAddApprover = !showAddApprover"
          >
            Add Approver
          </button>
        </div>

        <div v-if="showAddApprover" class="space-y-3">
          <input
            v-model="approverSearchQuery"
            class="input w-full"
            placeholder="Search users by name or email…"
            @input="onApproverSearch"
          />
          <div
            class="max-h-48 overflow-auto rounded-lg border border-border dark:border-border-dark divide-y divide-border/60 dark:divide-border-dark/60"
          >
            <label
              v-for="candidate in filteredApproverCandidates"
              :key="candidate.id"
              class="flex items-center gap-3 px-3 py-2.5 text-sm hover:bg-surface-container-low dark:hover:bg-slate-800 cursor-pointer"
            >
              <input
                type="checkbox"
                class="h-4 w-4 accent-primary shrink-0"
                :checked="!!pendingApproverIds[candidate.id]"
                @change="toggleApproverCandidate(candidate, $event)"
              />
              <div class="flex-1 min-w-0">
                <p class="font-medium truncate">{{ candidate.label }}</p>
                <p class="text-xs text-muted">{{ candidate.secondary ?? "" }}</p>
              </div>
            </label>
            <div v-if="!filteredApproverCandidates.length" class="px-3 py-4 text-center text-sm text-muted">
              No matching users found.
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button
              class="btn-primary btn-md"
              :disabled="!selectedApproverCandidateIds.length || isAddingApprovers"
              @click="batchAddApprovers"
            >
              {{ isAddingApprovers ? "Adding…" : `Add ${selectedApproverCandidateIds.length}` }}
            </button>
            <button class="btn-ghost btn-md" @click="cancelAddApprover">Cancel</button>
          </div>
        </div>

        <div class="space-y-2">
          <div
            v-for="approver in uat.approvers"
            :key="approver.id"
            class="flex items-center justify-between rounded-lg border border-border dark:border-border-dark p-3"
          >
            <div>
              <p class="text-sm font-semibold">{{ approver.userFullName }}</p>
              <p class="text-xs text-muted">{{ approver.userEmail }}</p>
            </div>
            <div class="flex items-center gap-2">
              <button
                v-if="canManage && !uat.readOnly && approver.status === 'PENDING'"
                class="btn-ghost btn-sm"
                @click="demoteApproverAction(approver.id)"
              >
                Demote to Watcher
              </button>
              <div class="flex items-center gap-2 mt-1">
                <span class="text-xs px-2 py-0.5 rounded-md font-medium" :class="approverStatusClass(approver.status)">
                  {{ formatEnumLabel(approver.status) }}
                </span>
                <span v-if="approver.decidedAt" class="text-xs text-muted">{{ approver.decidedAt }}</span>
              </div>
              <div v-if="approver.rejectionReason" class="text-xs text-danger mt-1">
                {{ approver.rejectionReason }}
              </div>
            </div>
          </div>
          <div v-if="!uat.approvers?.length" class="text-sm text-muted">
            No approvers added yet.
          </div>
        </div>
      </div>

      <div v-if="!uat.readOnly" class="card p-5 space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="font-semibold">UAT Watchers</h3>
          <button
            v-if="canManage"
            class="btn-ghost btn-md"
            @click="showAddWatcher = !showAddWatcher"
          >
            Add UAT Watcher
          </button>
        </div>

        <div v-if="showAddWatcher" class="space-y-3">
          <input
            v-model="watcherSearchQuery"
            class="input w-full"
            placeholder="Search users by name or email…"
          />
          <div
            class="max-h-48 overflow-auto rounded-lg border border-border dark:border-border-dark divide-y divide-border/60 dark:divide-border-dark/60"
          >
            <label
              v-for="candidate in filteredWatcherCandidates"
              :key="candidate.id"
              class="flex items-center gap-3 px-3 py-2.5 text-sm hover:bg-surface-container-low dark:hover:bg-slate-800 cursor-pointer"
            >
              <input
                type="checkbox"
                class="h-4 w-4 accent-primary shrink-0"
                :checked="!!pendingWatcherIds[candidate.id]"
                @change="toggleWatcherCandidate(candidate, $event)"
              />
              <div class="flex-1 min-w-0">
                <p class="font-medium truncate">{{ candidate.label }}</p>
                <p class="text-xs text-muted">{{ candidate.secondary ?? "" }}</p>
              </div>
            </label>
            <div v-if="!filteredWatcherCandidates.length" class="px-3 py-4 text-center text-sm text-muted">
              No matching users found.
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button
              class="btn-primary btn-md"
              :disabled="!selectedWatcherCandidateIds.length || isAddingWatchers"
              @click="batchAddWatchers"
            >
              {{ isAddingWatchers ? "Adding…" : `Add ${selectedWatcherCandidateIds.length}` }}
            </button>
            <button class="btn-ghost btn-md" @click="cancelAddWatcher">Cancel</button>
          </div>
        </div>

        <div class="space-y-2">
          <div
            v-for="w in watchers"
            :key="w.id"
            class="flex items-center justify-between rounded-lg border border-border dark:border-border-dark p-3"
          >
            <div>
              <p class="text-sm font-semibold">{{ w.userFullName }}</p>
              <p class="text-xs text-muted">{{ w.userEmail }}</p>
            </div>
            <div v-if="canManage" class="flex items-center gap-2">
              <button class="btn-ghost btn-sm" @click="promoteWatcherAction(w.userId)">
                Promote to Approver
              </button>
              <button class="btn-ghost btn-sm" @click="removeWatcherAction(w.userId)">
                Remove
              </button>
            </div>
          </div>
          <div v-if="!watchers.length" class="text-sm text-muted">
            No watchers added yet.
          </div>
        </div>
      </div>

      <div v-if="!uat.readOnly && canManage" class="flex flex-wrap gap-2">
        <button
          class="btn-primary btn-md"
          :disabled="isPromoting"
          @click="showPromoteConfirm = true"
        >
          {{ isPromoting ? "Promoting…" : "Promote to Deployment" }}
        </button>
      </div>

      <div v-if="currentApprover" class="border-t border-border dark:border-border-dark pt-4 mt-4">
        <p class="text-sm font-medium mb-2">Your Sign-Off</p>
        <div class="flex flex-wrap gap-2">
          <button
            class="btn-success btn-md"
            :disabled="isApproving"
            @click="doApprove"
          >
            {{ isApproving ? "Signing off…" : "Sign-Off" }}
          </button>
          <button
            class="btn-danger-ghost btn-md"
            :disabled="isRejecting"
            @click="showRejectForm = true"
          >
            {{ isRejecting ? "Rejecting…" : "Reject" }}
          </button>
        </div>
        <div v-if="showRejectForm" class="mt-2 flex gap-2">
          <input
            v-model="rejectionReason"
            class="input flex-1"
            placeholder="Reason for rejection"
            @keydown.enter="doReject"
          />
          <button class="btn-danger btn-md" :disabled="!rejectionReason.trim()" @click="doReject">
            Confirm
          </button>
          <button class="btn-ghost btn-md" @click="showRejectForm = false; rejectionReason = ''">
            Cancel
          </button>
        </div>
      </div>

      <div v-if="canRequesterSignOff" class="border-t border-border dark:border-border-dark pt-4 mt-4">
        <p class="text-sm font-medium mb-2">Requester Sign-Off</p>
        <p class="text-xs text-muted mb-2">
          Sign-off to acknowledge your satisfaction with the UAT results.
        </p>
        <button
          class="btn-success btn-md"
          :disabled="isSigningOff"
          @click="doRequesterSignOff"
        >
          {{ isSigningOff ? "Signing off…" : "Sign-Off" }}
        </button>
      </div>

      <div v-if="uat?.requesterSignedOff" class="border-t border-border dark:border-border-dark pt-4 mt-4">
        <div class="flex items-center gap-2">
          <span class="text-xs px-2 py-1 rounded-md bg-success/15 text-success font-medium">Signed Off ✓</span>
          <span class="text-xs text-muted">Requester has acknowledged the UAT results.</span>
        </div>
      </div>

      <div class="card p-5 space-y-4">
        <h3 class="font-semibold">Comments</h3>
        <div class="space-y-3">
          <div
            v-for="comment in uatComments"
            :key="comment.id"
            class="border border-border dark:border-border-dark rounded-lg p-3"
          >
            <p class="text-xs text-muted">
              {{ comment.author?.fullName ?? "Unknown" }} •
              {{ formatDateTimeInTenantTimezone(comment.createdAt) }}
            </p>
            <div
              class="text-sm mt-2 text-gray-800 dark:text-gray-200 rich-content"
              v-html="normalizeHtml(comment.body)"
            />
          </div>
          <div v-if="!uatComments.length" class="text-sm text-muted">
            No comments yet.
          </div>
        </div>

        <div class="space-y-2">
          <div class="border border-border dark:border-border-dark rounded-lg overflow-hidden">
            <SharedRichTextToolbar :editor="commentEditor" />
            <EditorContent
              :editor="commentEditor"
              class="rich-editor-content min-h-[80px] p-3"
            />
          </div>
          <div class="flex justify-end">
            <button
              class="btn-primary btn-md"
              :disabled="isPostingComment"
              @click="postUatCommentAction"
            >
              {{ isPostingComment ? "Posting…" : "Post Comment" }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <SharedAppModal
      :open="showPromoteConfirm"
      title="Promote UAT to Deployment"
      @close="showPromoteConfirm = false"
    >
      <p class="text-sm text-muted">
        This will finalize the UAT and promote it to deployment. This action cannot be undone.
      </p>
      <template #footer>
        <button class="btn-ghost btn-md" @click="showPromoteConfirm = false">Cancel</button>
        <button class="btn-primary btn-md" @click="doPromote">Confirm Promote</button>
      </template>
    </SharedAppModal>
  </div>
</template>

<script setup lang="ts">
import type { Uat, UatWatcher, ApproverCandidate, Comment } from "~/types";
import { EditorContent, useEditor } from "@tiptap/vue-3";
import { buildRichTextExtensions, normalizeRichTextHtml } from "~/composables/richText";
import { buildMentionExtension } from "~/composables/useMentions";

const props = defineProps<{
  requestId: string;
  approvalStatus: string | null;
  workflowMode: string | null;
}>();

const emit = defineEmits<{
  (e: "updated"): void;
}>();

const { error: toastError } = useToast();
const auth = useAuthStore();

const {
  getUat,
  createUat,
  updateUat,
  addUatApprover,
  listUatApprovers,
  promoteUat,
  searchApproverCandidates,
  listUatComments,
  postUatComment,
  approveUat,
  rejectUat,
  listUatWatchers,
  addUatWatchers,
  removeUatWatcher,
  promoteUatWatcher,
  demoteUatApprover,
} = useChangeRequests();

const uat = ref<Uat | null>(null);
const loading = ref(true);
const showCreateForm = ref(false);
const isEditing = ref(false);
const isSaving = ref(false);
const isPromoting = ref(false);
const isAddingApprovers = ref(false);
const showPromoteConfirm = ref(false);
const showAddApprover = ref(false);

const formTitle = ref("");
const approverSearchQuery = ref("");
const approverCandidates = ref<ApproverCandidate[]>([]);
const pendingApproverIds = ref<Record<string, ApproverCandidate>>({});

const uatComments = ref<Comment[]>([]);
const isPostingComment = ref(false);

const formEditor = useEditor({
  extensions: buildRichTextExtensions("Describe UAT plan, test cases, and acceptance criteria…"),
  editorProps: {
    attributes: {
      class: "prose dark:prose-invert max-w-none focus:outline-none min-h-[8rem]",
    },
  },
});

const commentEditor = useEditor({
  extensions: [...buildRichTextExtensions("Add a comment. Type @ to mention someone…"), buildMentionExtension()],
  editorProps: {
    attributes: {
      class: "prose dark:prose-invert max-w-none focus:outline-none min-h-[4rem]",
    },
  },
});

const isUatAvailable = computed(
  () => props.approvalStatus === "APPROVED" && props.workflowMode === "DELIVERY_PIPELINE",
);

const canCreateUat = computed(() => {
  return auth.hasPermission("cr.edit") || auth.isSuperAdmin || auth.isAdmin;
});

const canManage = computed(() => {
  if (!uat.value || uat.value.readOnly) return false;
  if (!auth.hasPermission("cr.manage_participants")) return false;
  return auth.isSuperAdmin || auth.isAdmin || uat.value.createdBy === auth.userId;
});

const currentApprover = computed(() => {
  if (!uat.value) return null;
  return uat.value.approvers.find(
    (a) => a.userId === auth.userId && a.status === 'PENDING'
  );
});

const canRequesterSignOff = computed(() => {
  if (!uat.value || uat.value.readOnly) return false;
  if (auth.role === 'Auditor') return false;
  if (uat.value.requesterSignedOff) return false;
  return uat.value.createdBy === auth.userId;
});

const statusClass = computed(() => {
  if (!uat.value) return "";
  switch (uat.value.status) {
    case "DRAFT":
      return "bg-primary/15 text-primary";
    case "APPROVED":
      return "bg-success/15 text-success";
    case "REJECTED":
      return "bg-danger/15 text-danger";
    default:
      return "bg-surface-container-high text-muted";
  }
});

const existingApproverUserIds = computed(() => {
  const ids = new Set<string>();
  for (const a of uat.value?.approvers ?? []) {
    ids.add(a.userId);
  }
  return ids;
});

const filteredApproverCandidates = computed(() => {
  const available = approverCandidates.value.filter(
    (c) => c.kind === "USER" && !existingApproverUserIds.value.has(c.id) && c.id !== auth.userId && c.role !== "Auditor",
  );
  const q = approverSearchQuery.value.trim().toLowerCase();
  if (!q) return available;
  return available.filter(
    (c) =>
      c.label.toLowerCase().includes(q) ||
      (c.secondary && c.secondary.toLowerCase().includes(q)),
  );
});

const selectedApproverCandidateIds = computed(() => Object.keys(pendingApproverIds.value));

function approverStatusClass(status: string) {
  switch (status) {
    case "APPROVED": return "bg-success/15 text-success";
    case "REJECTED": return "bg-danger/15 text-danger";
    default: return "bg-warning/15 text-warning";
  }
}

function formatEnumLabel(value: string) {
  return value
    .split("_")
    .filter(Boolean)
    .map((s) => s.charAt(0) + s.slice(1).toLowerCase())
    .join(" ");
}

function normalizeHtml(html: string | null | undefined) {
  return normalizeRichTextHtml(html);
}

const isApproving = ref(false);
const isRejecting = ref(false);
const showRejectForm = ref(false);
const rejectionReason = ref("");
const isSigningOff = ref(false);

const watchers = ref<UatWatcher[]>([]);
const showAddWatcher = ref(false);
const watcherSearchQuery = ref("");
const watcherCandidates = ref<ApproverCandidate[]>([]);
const pendingWatcherIds = ref<Record<string, ApproverCandidate>>({});
const isAddingWatchers = ref(false);

const existingUatWatcherUserIds = computed(() => {
  const ids = new Set<string>();
  for (const w of watchers.value) {
    ids.add(w.userId);
  }
  return ids;
});

const filteredWatcherCandidates = computed(() => {
  const available = watcherCandidates.value.filter(
    (c) =>
      c.kind === "USER" &&
      !existingApproverUserIds.value.has(c.id) &&
      !existingUatWatcherUserIds.value.has(c.id) &&
      c.id !== auth.userId &&
      c.role !== "Auditor",
  );
  const q = watcherSearchQuery.value.trim().toLowerCase();
  if (!q) return available;
  return available.filter(
    (c) =>
      c.label.toLowerCase().includes(q) ||
      (c.secondary && c.secondary.toLowerCase().includes(q)),
  );
});

const selectedWatcherCandidateIds = computed(() => Object.keys(pendingWatcherIds.value));

async function loadWatchers() {
  try {
    watchers.value = await listUatWatchers(props.requestId);
  } catch {
    watchers.value = [];
  }
}

function toggleWatcherCandidate(candidate: ApproverCandidate, event: Event) {
  const checked = (event.target as HTMLInputElement).checked;
  if (checked) {
    pendingWatcherIds.value = { ...pendingWatcherIds.value, [candidate.id]: candidate };
  } else {
    pendingWatcherIds.value = Object.fromEntries(
      Object.entries(pendingWatcherIds.value).filter(([key]) => key !== candidate.id),
    );
  }
}

function cancelAddWatcher() {
  showAddWatcher.value = false;
  watcherSearchQuery.value = "";
  pendingWatcherIds.value = {};
}

async function batchAddWatchers() {
  if (!selectedWatcherCandidateIds.value.length) return;
  isAddingWatchers.value = true;
  try {
    watchers.value = await addUatWatchers(props.requestId, selectedWatcherCandidateIds.value);
    cancelAddWatcher();
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to add UAT watchers."));
  } finally {
    isAddingWatchers.value = false;
  }
}

async function removeWatcherAction(userId: string) {
  try {
    await removeUatWatcher(props.requestId, userId);
    watchers.value = watchers.value.filter((w) => w.userId !== userId);
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to remove UAT watcher."));
  }
}

async function promoteWatcherAction(userId: string) {
  try {
    await promoteUatWatcher(props.requestId, userId);
    await loadUat();
    await loadWatchers();
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to promote UAT watcher."));
  }
}

async function demoteApproverAction(approverId: string) {
  try {
    await demoteUatApprover(props.requestId, approverId);
    await loadUat();
    await loadWatchers();
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to demote UAT approver."));
  }
}

async function doApprove() {
  isApproving.value = true;
  try {
    await approveUat(props.requestId);
    await loadUat();
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to sign off on UAT."));
  } finally {
    isApproving.value = false;
  }
}

async function doReject() {
  if (!rejectionReason.value.trim()) return;
  isRejecting.value = true;
  try {
    await rejectUat(props.requestId, rejectionReason.value.trim());
    showRejectForm.value = false;
    rejectionReason.value = "";
    await loadUat();
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to reject UAT."));
  } finally {
    isRejecting.value = false;
  }
}

async function doRequesterSignOff() {
  isSigningOff.value = true;
  try {
    await approveUat(props.requestId);
    await loadUat();
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to sign off on UAT."));
  } finally {
    isSigningOff.value = false;
  }
}

async function loadUat() {
  loading.value = true;
  try {
    uat.value = await getUat(props.requestId);
    if (uat.value && !uat.value.approvers?.length) {
      try {
        uat.value.approvers = await listUatApprovers(props.requestId);
      } catch {
        uat.value.approvers = [];
      }
    }
  } catch {
    uat.value = null;
  } finally {
    loading.value = false;
  }
}

function startEdit() {
  if (!uat.value) return;
  formTitle.value = uat.value.title;
  formEditor.value?.commands.setContent(uat.value.details ?? "");
  isEditing.value = true;
}

function cancelForm() {
  showCreateForm.value = false;
  isEditing.value = false;
  formTitle.value = "";
  formEditor.value?.commands.setContent("");
}

async function saveUat() {
  if (!formTitle.value.trim()) return;
  isSaving.value = true;
  try {
    const body = {
      title: formTitle.value.trim(),
      details: formEditor.value?.getHTML() ?? "",
    };
    if (uat.value) {
      uat.value = await updateUat(props.requestId, body);
    } else {
      uat.value = await createUat(props.requestId, body);
    }
    showCreateForm.value = false;
    isEditing.value = false;
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to save UAT."));
  } finally {
    isSaving.value = false;
  }
}

async function onApproverSearch() {
  if (!approverCandidates.value.length) {
    approverCandidates.value = await searchApproverCandidates("", 50);
  }
}

function toggleApproverCandidate(candidate: ApproverCandidate, event: Event) {
  const checked = (event.target as HTMLInputElement).checked;
  if (checked) {
    pendingApproverIds.value = { ...pendingApproverIds.value, [candidate.id]: candidate };
  } else {
    const copy = { ...pendingApproverIds.value };
    delete copy[candidate.id];
    pendingApproverIds.value = copy;
  }
}

function cancelAddApprover() {
  showAddApprover.value = false;
  approverSearchQuery.value = "";
  pendingApproverIds.value = {};
}

async function batchAddApprovers() {
  if (!selectedApproverCandidateIds.value.length) return;
  isAddingApprovers.value = true;
  try {
    for (const candidate of Object.values(pendingApproverIds.value)) {
      await addUatApprover(props.requestId, { userId: candidate.id });
    }
    await loadUat();
    cancelAddApprover();
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to add UAT approvers."));
  } finally {
    isAddingApprovers.value = false;
  }
}

async function doPromote() {
  isPromoting.value = true;
  try {
    await promoteUat(props.requestId);
    showPromoteConfirm.value = false;
    await loadUat();
    emit("updated");
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to promote UAT."));
  } finally {
    isPromoting.value = false;
  }
}

async function loadUatComments() {
  try {
    uatComments.value = await listUatComments(props.requestId);
  } catch {
    uatComments.value = [];
  }
}

async function postUatCommentAction() {
  if (!commentEditor.value) return;
  const body = commentEditor.value.getHTML().trim();
  if (!body || !commentEditor.value.getText().trim()) return;
  isPostingComment.value = true;
  try {
    await postUatComment(props.requestId, body);
    commentEditor.value.commands.clearContent();
    await loadUatComments();
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Unable to post your comment."));
  } finally {
    isPostingComment.value = false;
  }
}

watch(
  () => props.requestId,
  () => {
    if (isUatAvailable.value) {
      loadUat();
      loadUatComments();
      loadWatchers();
    } else {
      loading.value = false;
    }
  },
  { immediate: true },
);

watch(showAddApprover, async (isOpen) => {
  if (isOpen && !approverCandidates.value.length) {
    approverCandidates.value = await searchApproverCandidates("", 50);
  }
});

watch(showAddWatcher, async (isOpen) => {
  if (isOpen && !watcherCandidates.value.length) {
    watcherCandidates.value = await searchApproverCandidates("", 50);
  }
});
</script>
