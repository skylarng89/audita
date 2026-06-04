<template>
  <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
    <div class="card p-5 md:col-span-2 shadow-card-hover">
      <h2 class="font-semibold mb-2">Description</h2>
      <div
        v-if="changeRequest.description"
        class="text-sm text-gray-700 dark:text-gray-300 rich-content"
        v-html="renderedDescription"
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
          <dt class="text-muted w-32 shrink-0">Workflow Mode</dt>
          <dd class="font-medium">{{ changeRequest.workflowMode ?? "—" }}</dd>
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

    <div v-if="linkedRequestIds.length" class="card p-5">
      <h3 class="font-semibold mb-2">Linked Requests</h3>
      <div class="flex flex-wrap gap-1">
        <NuxtLink
          v-for="linkedId in linkedRequestIds"
          :key="linkedId"
          :to="`/change-requests/${linkedId}`"
          class="inline-flex items-center gap-1 rounded bg-primary/10 text-primary text-xs px-2 py-0.5 hover:bg-primary/20 transition-colors"
        >
          {{ linkedId }}
        </NuxtLink>
      </div>
    </div>

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
            <template v-if="localFieldValues[def.id] === 'true'">Yes</template>
            <template v-else-if="localFieldValues[def.id] === 'false'"
              >No</template
            >
            <template v-else>{{ localFieldValues[def.id] || "—" }}</template>
          </dd>
        </div>
      </dl>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import type {
  ChangeRequest,
  ChangeRequestCustomFieldValue,
  CustomFieldDefinition,
} from "~/types";
import { normalizeRichTextHtml } from "~/composables/richText";
import { formatDateTimeInTenantTimezone } from "~/composables/timezone";

const props = defineProps<{
  changeRequest: ChangeRequest;
  customFields: ChangeRequestCustomFieldValue[];
  fieldDefinitions: CustomFieldDefinition[];
  localFieldValues: Record<string, string>;
  linkedRequestIds: string[];
}>();

const renderedDescription = computed(() =>
  normalizeRichTextHtml(props.changeRequest.description),
);

function fmt(value: string | null) {
  return formatDateTimeInTenantTimezone(value);
}
</script>
