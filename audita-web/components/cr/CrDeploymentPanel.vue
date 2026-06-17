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

      <div v-if="deployment.approvers?.length" class="space-y-2">
        <h3 class="font-medium text-sm">Approvers</h3>
        <div
          v-for="approver in deployment.approvers"
          :key="approver.id"
          class="flex items-center justify-between rounded-lg border border-outline-variant/50 bg-surface-container-low px-3 py-2 dark:border-slate-600 dark:bg-slate-800/70"
        >
          <div>
            <p class="text-sm font-medium text-on-surface dark:text-gray-100">
              {{ approver.userFullName }}
            </p>
            <p class="text-xs text-muted">{{ approver.userEmail }}</p>
          </div>
          <div class="text-right">
            <p class="text-sm font-medium">{{ formatEnumLabel(approver.status) }}</p>
            <p v-if="approver.decidedAt" class="text-xs text-muted">
              {{ formatDateTime(approver.decidedAt) }}
            </p>
            <p v-if="approver.rejectionReason" class="text-xs text-danger">
              {{ approver.rejectionReason }}
            </p>
          </div>
        </div>
      </div>

      <div v-if="canAct" class="flex items-center gap-2 pt-2">
        <button
          class="btn-primary btn-md"
          :disabled="acting"
          @click="handleApprove"
        >
          Approve
        </button>
        <button
          class="btn-ghost btn-md"
          :disabled="acting"
          @click="showRejectModal = true"
        >
          Reject
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

      <SharedAppModal
        :open="showRejectModal"
        title="Reject Deployment"
        @close="showRejectModal = false"
      >
        <div class="space-y-3">
          <p class="text-sm text-muted">
            Provide a reason for rejecting this deployment.
          </p>
          <div>
            <label for="deploy-reject-reason" class="field-label">
              Reason <span class="text-danger">*</span>
            </label>
            <textarea
              id="deploy-reject-reason"
              v-model="rejectReason"
              class="input mt-1"
              rows="4"
              placeholder="Describe why this deployment is being rejected…"
            />
          </div>
        </div>
        <template #footer>
          <button class="btn-ghost btn-md" @click="showRejectModal = false">
            Cancel
          </button>
          <button class="btn-primary btn-md" @click="handleReject">
            Confirm Reject
          </button>
        </template>
      </SharedAppModal>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { Deployment, Comment } from "~/types";
import { EditorContent, useEditor } from "@tiptap/vue-3";
import { buildRichTextExtensions, normalizeRichTextHtml } from "~/composables/richText";

const props = defineProps<{
  requestId: string;
}>();

const { getDeployment, approveDeployment, rejectDeployment, listDeploymentComments, postDeploymentComment } =
  useChangeRequests();
const { error: toastError } = useToast();
const auth = useAuthStore();

const deployment = ref<Deployment | null>(null);
const loading = ref(true);
const acting = ref(false);
const showRejectModal = ref(false);
const rejectReason = ref("");
const deploymentComments = ref<Comment[]>([]);
const isPostingComment = ref(false);

const deploymentCommentEditor = useEditor({
  extensions: buildRichTextExtensions("Add a comment…"),
  editorProps: {
    attributes: {
      class: "prose dark:prose-invert max-w-none focus:outline-none min-h-[4rem]",
    },
  },
});

const canAct = computed(() => {
  if (!deployment.value || deployment.value.status !== "PENDING_APPROVAL") return false;
  if (auth.isSuperAdmin || auth.role === "Admin") return true;
  return deployment.value.approvers?.some(
    (a) => a.userId === auth.userId && a.status === "PENDING",
  );
});

const statusClasses = computed(() => {
  switch (deployment.value?.status) {
    case "APPROVED":
      return "bg-success/15 text-success";
    case "REJECTED":
      return "bg-danger/15 text-danger";
    default:
      return "bg-primary/15 text-primary";
  }
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

async function handleApprove() {
  acting.value = true;
  try {
    await approveDeployment(props.requestId);
    await load();
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to approve deployment."));
  } finally {
    acting.value = false;
  }
}

async function handleReject() {
  if (!rejectReason.value.trim()) {
    toastError("A rejection reason is required.");
    return;
  }
  acting.value = true;
  try {
    await rejectDeployment(props.requestId, rejectReason.value);
    showRejectModal.value = false;
    rejectReason.value = "";
    await load();
  } catch (err: unknown) {
    toastError(resolveApiErrorMessage(err, "Failed to reject deployment."));
  } finally {
    acting.value = false;
  }
}

onMounted(() => {
  load();
  loadDeploymentComments();
});
</script>
