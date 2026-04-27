<template>
  <div class="max-w-xl mx-auto">
    <div class="flex items-center gap-3 mb-6">
      <NuxtLink
        to="/platform/tenants"
        class="text-muted hover:text-foreground text-sm"
        >← Back</NuxtLink
      >
      <h1 class="text-2xl font-bold">Provision New Organisation</h1>
    </div>

    <div class="card p-6 space-y-4">
      <div
        v-if="error"
        class="rounded-md bg-danger-light border border-danger-border px-4 py-3 text-sm text-danger"
      >
        {{ error }}
      </div>

      <form @submit.prevent="handleSubmit" novalidate class="space-y-4">
        <div>
          <label
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Organisation Name
          </label>
          <input
            v-model="form.name"
            @input="suggestSlug"
            type="text"
            class="input"
            placeholder="Acme Corp"
            required
          />
        </div>

        <div>
          <label
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Slug
            <span class="text-muted font-normal normal-case"
              >(used in URLs, lowercase, hyphens only)</span
            >
          </label>
          <input
            v-model="form.slug"
            type="text"
            class="input font-mono"
            placeholder="acme-corp"
            required
            pattern="[a-z0-9-]+"
          />
        </div>

        <div>
          <label
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Admin Email
          </label>
          <input
            v-model="form.adminEmail"
            type="email"
            class="input"
            placeholder="admin@acme.com"
            required
          />
        </div>

        <div>
          <label
            class="block text-xs font-semibold uppercase tracking-wide text-muted mb-1.5"
          >
            Admin Full Name
          </label>
          <input
            v-model="form.adminFullName"
            type="text"
            class="input"
            placeholder="Jane Smith"
            required
          />
        </div>

        <button
          type="submit"
          class="btn-primary btn-lg w-full !mt-6"
          :disabled="loading"
        >
          {{ loading ? "Provisioning…" : "Provision Organisation →" }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "platform" });

const api = useApi();
const router = useRouter();
const { addToast } = useToast();

const form = reactive({
  name: "",
  slug: "",
  adminEmail: "",
  adminFullName: "",
});
const error = ref("");
const loading = ref(false);

function suggestSlug() {
  form.slug = form.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "");
}

async function handleSubmit() {
  error.value = "";
  loading.value = true;
  try {
    await api("/api/platform/v1/tenants", {
      method: "POST",
      body: form,
    });
    addToast({
      type: "success",
      message: `Organisation "${form.name}" provisioned. Invite email sent.`,
    });
    router.push("/platform/tenants");
  } catch (e: unknown) {
    const err = e as { data?: { message?: string } };
    error.value = err.data?.message ?? "Provisioning failed. Please try again.";
  } finally {
    loading.value = false;
  }
}
</script>
