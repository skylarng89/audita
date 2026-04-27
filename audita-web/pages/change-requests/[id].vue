<template>
  <div v-if="changeRequest" class="space-y-6">
    <div class="flex items-start justify-between">
      <div>
        <p class="text-xs text-muted uppercase tracking-widest mb-1">
          Change Request
        </p>
        <h1 class="text-3xl font-bold text-gray-900 dark:text-gray-100">
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
        class="btn-ghost"
        :class="{ 'ring-2 ring-primary': tab === 'details' }"
        @click="tab = 'details'"
      >
        Details
      </button>
      <button
        class="btn-ghost"
        :class="{ 'ring-2 ring-primary': tab === 'approvers' }"
        @click="tab = 'approvers'"
      >
        Approvers
      </button>
      <button
        class="btn-ghost"
        :class="{ 'ring-2 ring-primary': tab === 'activity' }"
        @click="tab = 'activity'"
      >
        Activity
      </button>
    </div>

    <section
      v-if="tab === 'details'"
      class="grid grid-cols-1 md:grid-cols-2 gap-4"
    >
      <div class="card p-5 md:col-span-2">
        <h2 class="font-semibold mb-2">Description</h2>
        <p class="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">
          {{ changeRequest.description || "No description." }}
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
          <button class="btn-ghost" @click="addCustomField">
            Add Custom Field Row
          </button>
        </div>
        <div class="mt-4">
          <button class="btn-primary" @click="saveCustomFieldsAction">
            Save Custom Fields
          </button>
        </div>
      </div>

      <div class="card p-5 md:col-span-2 flex flex-wrap gap-2">
        <button
          class="btn-primary"
          :disabled="changeRequest.status !== 'DRAFT'"
          @click="submitCr"
        >
          Submit
        </button>
        <button
          class="btn-ghost"
          :disabled="changeRequest.status === 'CANCELLED'"
          @click="cancelCr"
        >
          Cancel
        </button>
        <button
          class="btn-ghost"
          :disabled="changeRequest.status !== 'PENDING_APPROVAL'"
          @click="approveCr"
        >
          Approve
        </button>
        <button
          class="btn-ghost"
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
        <button class="btn-ghost" @click="showAddApprover = !showAddApprover">
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
        <button class="btn-primary" @click="addApproverAction">Save</button>
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
              class="btn-ghost"
              @click="moveUp(a.id)"
              :disabled="a.position === 1"
            >
              Up
            </button>
            <button class="btn-ghost" @click="removeApproverAction(a.id)">
              Remove
            </button>
          </div>
        </div>
      </div>
    </section>

    <section v-else class="card p-5">
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
          <button class="btn-ghost" @click="showReject = false">Close</button>
          <button class="btn-primary" @click="rejectCr">Confirm Reject</button>
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
  ChangeRequest,
  ChangeRequestCustomFieldValue,
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
  listActivity,
} = useChangeRequests();

const changeRequest = ref<ChangeRequest | null>(null);
const approvers = ref<CrApprover[]>([]);
const customFields = ref<ChangeRequestCustomFieldValue[]>([]);
const activity = ref<ActivityEntry[]>([]);
const tab = ref<"details" | "approvers" | "activity">("details");

const showAddApprover = ref(false);
const newApproverUserId = ref("");
const newApproverRequired = ref(true);

const showReject = ref(false);
const rejectReason = ref("");

async function loadAll() {
  changeRequest.value = await get(id.value);
  approvers.value = await listApprovers(id.value);
  customFields.value = await listCustomFields(id.value);
  activity.value = await listActivity(id.value);
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
  const index = order.findIndex((value) => value === approverId);
  if (index <= 0) {
    return;
  }
  const previous = order[index - 1];
  order[index - 1] = approverId;
  order[index] = previous;
  approvers.value = await reorderApprovers(id.value, order);
  activity.value = await listActivity(id.value);
}

onMounted(loadAll);
</script>
