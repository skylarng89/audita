<template>
  <div class="space-y-6">
    <div class="flex items-center gap-3">
      <NuxtLink
        to="/platform/tenants"
        class="text-muted hover:text-on-surface text-sm"
        >← Back</NuxtLink
      >
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          Organization Settings
        </p>
        <h1 class="text-3xl font-bold tracking-tight">{{ tenant?.name }}</h1>
        <div class="flex items-center gap-2 mt-1">
          <span class="text-xs font-mono text-muted">{{ tenant?.slug }}</span>
          <AppBadge
            v-if="tenant"
            :variant="tenant.status === 'ACTIVE' ? 'success' : 'danger'"
            >{{ tenant.status }}</AppBadge
          >
        </div>
      </div>
    </div>

    <div class="flex gap-1 border-b border-border dark:border-border-dark">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        @click="activeTab = tab.key"
        class="px-4 py-2 text-sm font-medium transition-colors rounded-t-md"
        :class="
          activeTab === tab.key
            ? 'text-primary border-b-2 border-primary -mb-px bg-primary/5'
            : 'text-muted hover:text-on-surface hover:bg-surface-container'
        "
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Overview -->
    <div
      v-if="activeTab === 'overview'"
      class="grid grid-cols-1 sm:grid-cols-2 gap-4"
    >
      <div class="card p-5 shadow-card-hover">
        <p
          class="text-xs font-semibold text-muted uppercase tracking-wide mb-1"
        >
          Status
        </p>
        <div class="flex items-center gap-2 mt-2">
          <AppBadge
            v-if="tenant"
            :variant="tenant.status === 'ACTIVE' ? 'success' : 'danger'"
            >{{ tenant.status }}</AppBadge
          >
          <button
            v-if="tenant?.status === 'ACTIVE'"
            @click="suspendTenant"
            class="btn-ghost btn-sm text-danger"
          >
            Suspend
          </button>
          <button
            v-else
            @click="activateTenant"
            class="btn-ghost btn-sm text-success"
          >
            Reactivate
          </button>
        </div>
      </div>
      <div class="card p-5 shadow-card-hover">
        <p
          class="text-xs font-semibold text-muted uppercase tracking-wide mb-1"
        >
          Created
        </p>
        <p class="mt-2 text-sm">
          {{ tenant?.createdAt ? formatDate(tenant.createdAt) : "—" }}
        </p>
      </div>
    </div>

    <!-- Domain Whitelist -->
    <div v-if="activeTab === 'domains'" class="card shadow-card-hover">
      <div
        class="flex items-center justify-between px-5 py-4 border-b border-border dark:border-border-dark"
      >
        <h2 class="font-semibold">Allowed Domains</h2>
        <div class="flex gap-2">
          <input
            v-model="newDomain"
            type="text"
            class="input py-1.5 text-sm w-56"
            placeholder="acme.com"
            @keyup.enter="addDomain"
          />
          <button @click="addDomain" class="btn-primary btn-sm">Add</button>
        </div>
      </div>
      <div class="p-5 flex flex-wrap gap-2">
        <span
          v-for="d in domains"
          :key="d.id"
          class="inline-flex items-center gap-1.5 px-3 py-1 bg-muted/10 rounded-full text-sm"
        >
          {{ d.domain }}
          <button
            @click="removeDomain(d.id)"
            class="text-muted hover:text-danger"
            aria-label="Remove"
          >
            ×
          </button>
        </span>
        <span v-if="domains.length === 0" class="text-sm text-muted"
          >No domains added yet.</span
        >
      </div>
    </div>

    <!-- SSO Configuration -->
    <div v-if="activeTab === 'sso'" class="space-y-4">
      <div v-for="provider in ['GOOGLE', 'MICROSOFT']" :key="provider"
        class="card p-5 shadow-card-hover">
        <h3 class="font-semibold mb-4">{{ provider === 'GOOGLE' ? 'Google' : 'Microsoft Azure AD' }}</h3>
        <div class="space-y-3">
          <div>
            <label class="text-xs font-semibold text-muted uppercase tracking-wide">Client ID</label>
            <input
              v-model="ssoForms[provider].clientId"
              type="text"
              class="input mt-1"
              :placeholder="provider === 'GOOGLE' ? 'google-client-id.apps.googleusercontent.com' : 'azure-app-client-id'"
            />
          </div>
          <div>
            <label class="text-xs font-semibold text-muted uppercase tracking-wide">Client Secret</label>
            <input
              v-model="ssoForms[provider].clientSecret"
              type="password"
              class="input mt-1"
              placeholder="••••••••"
            />
          </div>
          <div v-if="provider === 'MICROSOFT'">
            <label class="text-xs font-semibold text-muted uppercase tracking-wide">Tenant ID</label>
            <input
              v-model="ssoForms[provider].msTenantId"
              type="text"
              class="input mt-1"
              placeholder="common or tenant-id"
            />
          </div>
          <button @click="saveSso(provider)" class="btn-primary btn-sm">
            Save SSO Config
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "platform" });

import { useLoadingOverlay } from "~/composables/useLoadingOverlay";

const api = useApi();
const route = useRoute();
const { success: toastSuccess, error: toastError } = useToast();
const tenantId = route.params.id as string;

interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: string;
  createdAt: string;
}
interface Domain {
  id: string;
  domain: string;
  createdAt: string;
}

const activeTab = ref("overview");
const tabs = [
  { key: "overview", label: "Overview" },
  { key: "domains", label: "Domain Whitelist" },
  { key: "sso", label: "SSO Configuration" },
];

const { data: tenant, pending, refresh: refreshTenant } = await useAsyncData<Tenant>(
  `tenant-${tenantId}`,
  () => api(`/api/platform/v1/tenants/${tenantId}`) as Promise<Tenant>,
);

const { data: domainData, refresh: refreshDomains } = await useAsyncData<
  Domain[]
>(
  `tenant-domains-${tenantId}`,
  () =>
    api(`/api/platform/v1/tenants/${tenantId}/domains`) as Promise<Domain[]>,
);

const { hide: hideLoading } = useLoadingOverlay();
watch(pending, (val) => { if (!val) hideLoading(); });

const domains = computed(() => domainData.value ?? []);

const newDomain = ref("");

const ssoForms = reactive<Record<string, { clientId: string; clientSecret: string; msTenantId: string }>>({
  GOOGLE: { clientId: "", clientSecret: "", msTenantId: "" },
  MICROSOFT: { clientId: "", clientSecret: "", msTenantId: "" },
});

async function addDomain() {
  if (!newDomain.value.trim()) return;
  try {
    await api(`/api/platform/v1/tenants/${tenantId}/domains`, {
      method: "POST",
      body: { domain: newDomain.value.trim() },
    });
    newDomain.value = "";
    refreshDomains();
  } catch (error: unknown) {
    toastError(resolveApiErrorMessage(error, "Failed to add domain."));
  }
}

async function removeDomain(domainId: string) {
  try {
    await api(`/api/platform/v1/tenants/${tenantId}/domains/${domainId}`, {
      method: "DELETE",
    });
    refreshDomains();
  } catch (error: unknown) {
    toastError(resolveApiErrorMessage(error, "Failed to remove domain."));
  }
}

async function suspendTenant() {
  await api(`/api/platform/v1/tenants/${tenantId}`, {
    method: "PATCH",
    body: { status: "SUSPENDED" },
  });
  refreshTenant();
}

async function activateTenant() {
  await api(`/api/platform/v1/tenants/${tenantId}`, {
    method: "PATCH",
    body: { status: "ACTIVE" },
  });
  refreshTenant();
}

async function saveSso(provider: string) {
  const form = ssoForms[provider];
  try {
    await api(`/api/platform/v1/tenants/${tenantId}/sso`, {
      method: "PUT",
      body: {
        provider,
        clientId: form.clientId,
        clientSecret: form.clientSecret,
        msTenantId: provider === "MICROSOFT" ? form.msTenantId : null,
      },
    });
    toastSuccess(`${provider === "GOOGLE" ? "Google" : "Microsoft"} SSO configuration saved.`);
  } catch (error: unknown) {
    toastError(resolveApiErrorMessage(error, "Failed to save SSO configuration."));
  }
}

function formatDate(iso: string) {
  return formatDateInTenantTimezone(iso);
}
</script>
