<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          IAM Framework
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Group Management</h1>
        <p class="text-sm text-muted mt-1">
          Organise users into groups for bulk approvals and notifications.
        </p>
      </div>
      <button
        @click="showCreateModal = true"
        class="btn-primary btn-sm shadow-lg shadow-primary/20"
      >
        + New Group
      </button>
    </div>

    <div class="card shadow-card-hover">
      <AppTable :columns="columns" :rows="groups" :loading="pending">
        <template #cell-actions="{ row }">
          <div class="flex gap-2">
            <button
              @click="openDetail(row)"
              class="text-xs text-primary hover:underline"
            >
              Members
            </button>
            <button
              @click="deleteGroup(row.id)"
              class="text-xs text-danger hover:underline"
            >
              Delete
            </button>
          </div>
        </template>
      </AppTable>
      <div class="px-5 py-4 border-t border-border dark:border-border-dark">
        <AppPagination
          :current-page="page"
          :total-pages="totalPages"
          @change="
            (p) => {
              page = p;
              refresh();
            }
          "
        />
      </div>
    </div>

    <!-- Create Group Modal -->
    <AppModal
      v-if="showCreateModal"
      title="New Group"
      @close="showCreateModal = false"
    >
      <form @submit.prevent="createGroup" class="space-y-4 p-4">
        <div>
          <label
            class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5"
            >Name</label
          >
          <input v-model="createForm.name" type="text" class="input" required />
        </div>
        <div>
          <label
            class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5"
            >Description</label
          >
          <input v-model="createForm.description" type="text" class="input" />
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <button
            type="button"
            @click="showCreateModal = false"
            class="btn-ghost btn-sm"
          >
            Cancel
          </button>
          <button type="submit" class="btn-primary btn-sm">Create</button>
        </div>
      </form>
    </AppModal>

    <!-- Group Members Drawer -->
    <AppModal
      v-if="detailGroup"
      :title="`${detailGroup.name} — Members`"
      @close="detailGroup = null"
    >
      <div class="p-4 space-y-3">
        <div class="flex gap-2">
          <input
            v-model="addMemberId"
            type="text"
            class="input text-sm"
            placeholder="User ID…"
          />
          <button @click="addMember" class="btn-primary btn-sm">Add</button>
        </div>
        <div v-if="membersLoading" class="text-sm text-muted">Loading…</div>
        <ul v-else class="divide-y divide-border dark:divide-border-dark">
          <li
            v-for="m in members"
            :key="m.id"
            class="flex items-center justify-between py-2 text-sm"
          >
            <span
              >{{ m.fullName }}
              <span class="text-muted text-xs">{{ m.email }}</span></span
            >
            <button
              @click="removeMember(m.id)"
              class="text-xs text-danger hover:underline"
            >
              Remove
            </button>
          </li>
          <li
            v-if="members.length === 0"
            class="py-3 text-sm text-muted text-center"
          >
            No members yet.
          </li>
        </ul>
      </div>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
definePageMeta({ layout: "default" });

const api = useApi();
const { success: toastSuccess, error: toastError } = useToast();

interface Group {
  id: string;
  name: string;
  description: string;
  createdAt: string;
}
interface Member {
  id: string;
  email: string;
  fullName: string;
}
interface PageData {
  content: Group[];
  totalPages: number;
}

const page = ref(0);
const { data, pending, refresh } = await useAsyncData<PageData>(
  "groups-list",
  () => api(`/api/v1/groups?page=${page.value}&size=20`) as Promise<PageData>,
);

const groups = computed(() => data.value?.content ?? []);
const totalPages = computed(() => data.value?.totalPages ?? 1);

const showCreateModal = ref(false);
const createForm = reactive({ name: "", description: "" });

const detailGroup = ref<Group | null>(null);
const members = ref<Member[]>([]);
const membersLoading = ref(false);
const addMemberId = ref("");

async function createGroup() {
  try {
    await api("/api/v1/groups", { method: "POST", body: createForm });
    toastSuccess("Group created.");
    showCreateModal.value = false;
    createForm.name = "";
    createForm.description = "";
    refresh();
  } catch {
    toastError("Failed to create group.");
  }
}

async function deleteGroup(id: string) {
  try {
    await api(`/api/v1/groups/${id}`, { method: "DELETE" });
    toastSuccess("Group deleted.");
    refresh();
  } catch {
    toastError("Failed to delete group.");
  }
}

async function openDetail(group: Group) {
  detailGroup.value = group;
  membersLoading.value = true;
  try {
    const res = (await api(
      `/api/v1/groups/${group.id}/members`,
    )) as PageData & { content: Member[] };
    members.value = res.content;
  } finally {
    membersLoading.value = false;
  }
}

async function addMember() {
  if (!detailGroup.value || !addMemberId.value.trim()) return;
  try {
    await api(`/api/v1/groups/${detailGroup.value.id}/members`, {
      method: "POST",
      body: { userId: addMemberId.value.trim() },
    });
    addMemberId.value = "";
    openDetail(detailGroup.value);
  } catch {
    toastError("Failed to add member.");
  }
}

async function removeMember(userId: string) {
  if (!detailGroup.value) return;
  try {
    await api(`/api/v1/groups/${detailGroup.value.id}/members/${userId}`, {
      method: "DELETE",
    });
    openDetail(detailGroup.value);
  } catch {
    toastError("Failed to remove member.");
  }
}

const columns = [
  { key: "name", label: "Name" },
  { key: "description", label: "Description" },
  { key: "actions", label: "" },
];
</script>
