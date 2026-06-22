<template>
  <div class="space-y-4">
    <div v-if="loading" class="text-sm text-muted py-8 text-center">
      Loading deployment…
    </div>

    <div
      v-else-if="!deployment"
      class="text-sm text-muted py-8 text-center"
      data-testid="no-deployment"
    >
      Deployment will appear when UAT is promoted
    </div>

    <template v-else>
      <div class="flex items-center justify-between">
        <h2 class="font-semibold">Deployment</h2>
        <span
          class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold"
          :class="statusClasses"
        >
          {{ deployment.status }}
        </span>
      </div>

      <dl class="space-y-1.5 text-sm">
        <div class="flex gap-2">
          <dt class="text-muted w-32 shrink-0">Promoted</dt>
          <dd class="font-medium">{{ formatDateTime(deployment.promotedAt) }}</dd>
        </div>
        <div class="flex gap-2">
          <dt class="text-muted w-32 shrink-0">Status</dt>
          <dd class="font-medium">{{ formatEnumLabel(deployment.status) }}</dd>
        </div>
      </dl>

      <div v-if="deployment.status === 'PENDING'" class="space-y-3">
        <h3 class="font-medium text-sm">Deployer</h3>
        <div
          v-if="deployment.assignee"
          class="flex items-center justify-between rounded-lg border border-border dark:border-border-dark p-3"
        >
          <div>
            <p class="text-sm font-semibold">{{ deployment.assignee.fullName }}</p>
            <p class="text-xs text-muted">{{ deployment.assignee.email }}</p>
          </div>
          <button
            v-if="canManage"
            class="btn-ghost btn-sm"
            @click="openAssignPicker"
          >
            Change Assignee
          </button>
        </div>
        <div v-else-if="canManage">
          <button class="btn-primary btn-md" @click="openAssignPicker">
            Assign Deployer
          </button>
        </div>
        <p v-else class="text-sm text-muted">
          No deployer assigned yet.
        </p>

        <div v-if="showAssignPicker" class="space-y-3">
          <input
            v-model="assigneeSearchQuery"
            class="input w-full"
            placeholder="Search users by name or email…"
          />
          <div
            class="max-h-48 overflow-auto rounded-lg border border-border dark:border-border-dark divide-y divide-border/60 dark:divide-border-dark/60"
          >
            <label
              v-for="candidate in filteredAssigneeCandidates"
              :key="candidate.id"
              class="flex items-center gap-3 px-3 py-2.5 text-sm hover:bg-surface-container-low dark:hover:bg-slate-800 cursor-pointer"
            >
              <input
                type="radio"
                name="deployer-assignee"
                class="h-4 w-4 accent-primary shrink-0"
                :checked="selectedAssigneeId === candidate.id"
                @change="selectedAssigneeId = candidate.id"
              />
              <div class="flex-1 min-w-0">
                <p class="font-medium truncate">{{ candidate.label }}</p>
                <p class="text-xs text-muted">{{ candidate.secondary ?? "" }}</p>
              </div>
            </label>
            <div
              v-if="!filteredAssigneeCandidates.length"
              class="px-3 py-4 text-center text-sm text-muted"
            >
              No matching users found.
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button
              class="btn-primary btn-md"
              :disabled="!selectedAssigneeId || isAssigning"
              @click="handleAssign"
            >
              {{ isAssigning ? "Assigning…" : "Assign" }}
            </button>
            <button class="btn-ghost btn-md" @click="cancelAssignPicker">
              Cancel
            </button>
          </div>
        </div>
      </div>

      <div v-else-if="deployment.assignee" class="space-y-1.5 text-sm">
        <div class="flex gap-2">
          <dt class="text-muted w-32 shrink-0">Deployer</dt>
          <dd class="font-medium">
            {{ deployment.assignee.fullName }}
            <span class="text-xs text-muted">({{ deployment.assignee.email }})</span>
          </dd>
        </div>
      </div>

      <div v-if="canCompleteDeployment" class="pt-2">
        <button
          class="btn-success btn-md"
          :disabled="isCompleting"
          @click="handleComplete"
        >
          {{ isCompleting ? "Completing…" : "Mark Deployment Completed" }}
        </button>
      </div>

      <div class="space-y-4 pt-4 border-t border-border dark:border-border-dark">
        <h3 class="font-semibold">Comments</h3>
        <div class="space-y-3">
          <div
            v-for="comment in deploymentComments"
            :key="comment.id"
            class="border border-border dark:border-border-dark rounded-lg p-3"
          >
            <p class="text-xs text-muted">
              {{ comment.author?.fullName ?? "Unknown" }} •
              {{ formatDateTimeInTenantTimezone(comment.createdAt) }}
            </p>
            <div
              class="text-sm mt-2 text-gray-800 dark:text-gray-200 rich-content"
              v-html="normalizeRichTextHtml(comment.body)"
            />
          </div>
          <div v-if="!deploymentComments.length" class="text-sm text-muted">
            No comments yet.
          </div>
        </div>

        <div class="space-y-2">
          <div class="border border-border dark:border-border-dark rounded-lg overflow-hidden">
            <SharedRichTextToolbar :editor="deploymentCommentEditor" />
            <EditorContent
              :editor="deploymentCommentEditor"
              class="rich-editor-content min-h-[80px] p-3"
            />
          </div>
          <div class="flex justify-end">
            <button
              class="btn-primary btn-md"
              :disabled="isPostingComment"
              @click="postDeploymentCommentAction"
            >
              {{ isPostingComment ? "Posting…" : "Post Comment" }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { Deployment, Comment, ApproverCandidate } from "~/types";
import { EditorContent, useEditor } from "@tiptap/vue-3";
import { buildRichTextExtensions, normalizeRichTextHtml } from "~/composables/richText";
import { buildMentionExtension } from "~/composables/useMentions";

const props = defineProps<{
  requestId: string;
  createdBy: string | null;
}>();

const {
  getDeployment,
  assignDeployer,
  completeDeployment,
  listDeploymentComments,
  postDeploymentComment,
  searchApproverCandidates,
} = useChangeRequests();
const { error: toastError } = useToast();
const auth = useAuthStore();

const deployment = ref<Deployment | null>(null);
const loading = ref(true);
const deploymentComments = ref<Comment[]>([]);
const isPostingComment = ref(false);
const isCompleting = ref(false);
const isAssigning = ref(false);
const showAssignPicker = ref(false);
const assigneeSearchQuery = ref("");
const assigneeCandidates = ref<ApproverCandidate[]>([]);
const selectedAssigneeId = ref<string | null>(null);

const deploymentCommentEditor = useEditor({
  extensions: [...buildRichTextExtensions("Add a comment. Type @ to mention someone…"), buildMentionExtension()],
  editorProps: {
    attributes: {
      class: "prose dark:prose-invert max-w-none focus:outline-none min-h-[4rem]",
    },
  },
});

const isCreator = computed(() => props.createdBy === auth.userId);

const canManage = computed(() => {
  if (!deployment.value || deployment.value.status !== "PENDING") return false;
  if (!auth.hasPermission("cr.manage_participants")) return false;
  return auth.isSuperAdmin || auth.isAdmin || isCreator.value;
});

const canCompleteDeployment = computed(() => {
  if (!deployment.value || deployment.value.status !== "PENDING") return false;
  if (!deployment.value.assignee) return false;
  return auth.userId === deployment.value.assignee.id || auth.isAdmin || auth.isSuperAdmin;
});

const statusClasses = computed(() => {
  switch (deployment.value?.status) {
    case "COMPLETED":
      return "bg-success/15 text-success";
    case "CANCELLED":
      return "bg-danger/15 text-danger";
    default:
      return "bg-primary/15 text-primary";
  }
});

const filteredAssigneeCandidates = computed(() => {
  const available = assigneeCandidates.value.filter(
    (c) => c.kind === "USER" && c.role !== "Auditor",
  );
  const q = assigneeSearchQuery.value.trim().toLowerCase();
  if (!q) return available;
  return available.filter(
    (c) =>
      c.label.toLowerCase().includes(q) ||
      (c.secondary && c.secondary.toLowerCase().includes(q)),
  );
});

function formatDateTime(value: string | null) {
  return formatDateTimeInTenantTimezone(value);
}

function formatEnumLabel(value: string | null | undefined) {
  if (!value) return "—";
  return value
    .split("_")
    .filter(Boolean)
    .map((s) => s.charAt(0) + s.slice(1).toLowerCase())
    .join(" ");
}

async function load() {
  loading.value = true;
  try {
    deployment.value = await getDeployment(props.requestId);
  } catch {
    deployment.value = null;
  } finally {
    loading.value = false;
  }
}

async function loadDeploymentComments() {
  try {
    deploymentComments.value = await listDeploymentComments(props.requestId);
  } catch {
    deploymentComments.value = [];
  }
}

async function loadAssigneeCandidates() {
  if (!assigneeCandidates.value.length) {
    assigneeCandidates.value = await searchApproverCandidates("", 50);
  }
}

async function openAssignPicker() {
  showAssignPicker.value = true;
  selectedAssigneeId.value = null;
  await loadAssigneeCandidates();
}

function cancelAssignPicker() {
  showAssignPicker.value = false;
  selectedAssigneeId.value = null;
  assigneeSearchQuery.value = "";
}

async function handleAssign() {
  if (!selectedAssigneeId.value) return;
  isAssigning.value = true;
  try {
    deployment.value = await assignDeployer(props.requestId, selectedAssigneeId.value);
    showAssignPicker.value = false;
    selectedAssigneeId.value = null;
    assigneeSearchQuery.value = "";
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to assign deployer."));
  } finally {
    isAssigning.value = false;
  }
}

async function handleComplete() {
  isCompleting.value = true;
  try {
    deployment.value = await completeDeployment(props.requestId);
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to complete deployment."));
  } finally {
    isCompleting.value = false;
  }
}

async function postDeploymentCommentAction() {
  if (!deploymentCommentEditor.value) return;
  const body = deploymentCommentEditor.value.getHTML().trim();
  if (!body || !deploymentCommentEditor.value.getText().trim()) return;
  isPostingComment.value = true;
  try {
    await postDeploymentComment(props.requestId, body);
    deploymentCommentEditor.value.commands.clearContent();
    await loadDeploymentComments();
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Unable to post your comment."));
  } finally {
    isPostingComment.value = false;
  }
}

onMounted(() => {
  load();
  loadDeploymentComments();
});
</script>
