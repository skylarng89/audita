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
        <h3 class="font-semibold mb-2">Scheduling</h3>
        <p class="text-sm">Start: {{ fmt(changeRequest.scheduledStart) }}</p>
        <p class="text-sm">End: {{ fmt(changeRequest.scheduledEnd) }}</p>
        <p class="text-sm">
          SLA Deadline: {{ fmt(changeRequest.slaDeadline) }}
        </p>
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

      <div class="card p-5 md:col-span-2">
        <h3 class="font-semibold mb-3">Custom Fields</h3>
        <div class="space-y-3">
          <div
            v-for="field in customFields"
            :key="field.fieldId"
            class="grid grid-cols-[180px_1fr] gap-2 items-center"
          >
            <span class="text-xs text-muted font-mono">{{
              field.fieldId
            }}</span>
            <input v-model="field.value" class="input" />
          </div>
          <div v-if="!customFields.length" class="text-sm text-muted">
            No custom field values saved yet.
          </div>
          <button class="btn-ghost btn-md" @click="addCustomField">
            Add Custom Field Row
          </button>
        </div>
        <div class="mt-4">
          <button class="btn-primary btn-md" @click="saveCustomFieldsAction">
            Save Custom Fields
          </button>
        </div>
      </div>

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
            @change="onSelectUpload"
          />
          <button class="btn-ghost btn-md mt-3" @click="fileInput?.click()">
            Select File
          </button>
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
            <span class="text-xs text-muted">{{ fmt(file.createdAt) }}</span>
          </div>
          <div v-if="!attachments.length" class="text-sm text-muted">
            No attachments uploaded yet.
          </div>
        </div>
      </div>

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
} from "~/types";
import { format, parseISO } from "date-fns";

definePageMeta({ middleware: "auth" });

const route = useRoute();
const id = computed(() => String(route.params.id));
const {
  get,
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
  listActivity,
  listComments,
  postComment,
} = useChangeRequests();

const changeRequest = ref<ChangeRequest | null>(null);
const approvers = ref<CrApprover[]>([]);
const customFields = ref<ChangeRequestCustomFieldValue[]>([]);
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

async function loadAll() {
  changeRequest.value = await get(id.value);
  approvers.value = await listApprovers(id.value);
  customFields.value = await listCustomFields(id.value);
  attachments.value = await listAttachments(id.value);
  activity.value = await listActivity(id.value);
  comments.value = await listComments(id.value);
}

function fmt(value: string | null) {
  return value ? format(parseISO(value), "MMM d, yyyy HH:mm") : "—";
}

function addCustomField() {
  customFields.value.push({ fieldId: "", value: "" });
}

async function saveCustomFieldsAction() {
  customFields.value = await saveCustomFields(id.value, customFields.value);
  activity.value = await listActivity(id.value);
}

async function uploadSelected(file: File | null) {
  if (!file) {
    return;
  }
  await uploadAttachment(id.value, file);
  attachments.value = await listAttachments(id.value);
  activity.value = await listActivity(id.value);
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
</script>
