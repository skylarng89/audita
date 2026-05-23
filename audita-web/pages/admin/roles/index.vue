<template>
  <div class="space-y-6">
    <div>
      <p
        class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
      >
        Identity and Governance
      </p>
      <h1 class="text-3xl font-bold tracking-tight">Roles & Permissions</h1>
      <p class="text-sm text-muted mt-1">
        View the built-in roles and their associated permissions.
      </p>
    </div>

    <div v-if="pending" class="text-muted text-sm">Loading roles…</div>

    <div v-else class="space-y-4">
      <div
        v-for="role in roles"
        :key="role.id"
        class="card p-5 shadow-card-hover"
      >
        <div class="flex items-center gap-2 mb-3">
          <h2 class="font-semibold text-base">{{ role.name }}</h2>
          <AppBadge v-if="role.isSystem" variant="default">System</AppBadge>
        </div>
        <p v-if="role.description" class="text-sm text-muted mb-3">
          {{ role.description }}
        </p>
        <div class="flex flex-wrap gap-1.5">
          <span
            v-for="perm in role.permissions"
            :key="perm.id"
            class="inline-block px-2 py-0.5 text-xs font-mono bg-muted/10 rounded"
          >
            {{ perm.code }}
          </span>
          <span v-if="role.permissions.length === 0" class="text-xs text-muted"
            >No permissions assigned.</span
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ middleware: ["auth", "admin-only"] });

const api = useApi();

interface Permission {
  id: string;
  code: string;
  label: string;
}
interface Role {
  id: string;
  name: string;
  description: string;
  isSystem: boolean;
  permissions: Permission[];
}

const { data, pending } = await useAsyncData<Role[]>(
  "roles-list",
  () => api("/api/v1/roles") as Promise<Role[]>,
);
const roles = computed(() => data.value ?? []);
</script>
