<template>
  <div class="space-y-6">
    <div style="background:#dc2626;color:#fff;padding:5px 12px;font-size:12px;font-family:monospace;border-radius:6px;white-space:pre-wrap;word-break:break-all">
      DEBUG | pending={{ pending }} | groups.length={{ groups.length }} | data.contentLength={{ data?.content?.length ?? 'null' }} | totalElements={{ data?.totalElements ?? 'null' }} | loadError={{ loadError || 'none' }}
    </div>
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          IAM Framework
        </p>
        <h1 class="text-3xl font-bold tracking-tight">Groups</h1>
        <p class="text-sm text-muted mt-1">
          Organize review and approval ownership by team.
        </p>
      </div>
      <button
        v-if="auth.isAdmin"
        class="btn-primary btn-sm shadow-lg shadow-primary/20"
        @click="navigateTo('/groups/new')"
      >
        + New Group
      </button>
    </div>

    <div class="card shadow-card-hover">
      <div class="w-full overflow-x-auto">
        <table class="w-full min-w-[980px] table-auto divide-y divide-border dark:divide-border-dark">
          <thead>
            <tr>
              <th v-for="col in columns" :key="col.key" scope="col" class="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wide text-muted whitespace-nowrap">{{ col.label }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border dark:divide-border-dark">
            <template v-if="pending">
              <tr v-for="i in 5" :key="i" class="animate-pulse">
                <td v-for="col in columns" :key="col.key" class="px-6 py-4"><div class="h-4 rounded bg-gray-200 dark:bg-slate-700" /></td>
              </tr>
            </template>
            <tr v-else-if="groups.length === 0">
              <td :colspan="columns.length" class="px-6 py-12 text-center text-sm text-muted">No groups yet.</td>
            </tr>
            <tr v-for="row in groups" :key="row.id" class="hover:bg-gray-50 dark:hover:bg-slate-700/50 transition-colors">
              <td class="px-6 py-4 text-sm text-gray-700 dark:text-gray-300 align-middle">{{ row.name }}</td>
              <td class="px-6 py-4 text-sm text-gray-700 dark:text-gray-300 align-middle text-muted">{{ row.description || "—" }}</td>
              <td class="px-6 py-4 text-sm text-gray-700 dark:text-gray-300 align-middle">{{ row.memberCount ?? 0 }}</td>
              <td class="px-6 py-4 text-sm text-gray-700 dark:text-gray-300 align-middle text-muted">{{ formatDate(row.createdAt) }}</td>
              <td v-if="auth.isAdmin" class="px-6 py-4 text-sm text-gray-700 dark:text-gray-300 align-middle">
                <div class="flex gap-2">
                  <button class="text-xs text-primary hover:underline" @click.stop="toggleMemberPanel(row)">Manage Members</button>
                  <button class="text-xs text-danger hover:underline" @click.stop="openDeleteConfirm(row)">Delete</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        v-if="total > pageSize"
        class="border-t border-border px-5 py-4 dark:border-border-dark"
      >
        <AppPagination
          :page="page"
          :page-size="pageSize"
          :total="total"
          @update:page="onPageChange"
        />
      </div>
    </div>

    <div
      v-if="expandedGroup"
      class="card shadow-card-hover border-l-4 border-l-primary"
    >
      <div class="flex items-center justify-between px-6 py-4 border-b border-border dark:border-border-dark">
        <h2 class="text-lg font-semibold">
          {{ expandedGroup.name }} — Members
        </h2>
        <button
          class="btn-ghost btn-sm"
          @click="expandedGroup = null"
        >
          Close
        </button>
      </div>

      <div class="p-6 space-y-4">
        <div class="flex gap-2">
          <div class="relative flex-1">
            <input
              v-model="memberSearchQuery"
              type="text"
              class="input"
              placeholder="Search users to add…"
              @input="onMemberSearchInput"
              @focus="memberSearchOpen = !!memberSearchQuery"
            />
            <div
              v-if="memberSearchOpen && memberSearchResults.length"
              class="absolute z-30 mt-1 w-full bg-white dark:bg-slate-800 border border-border dark:border-border-dark rounded-lg shadow-lg max-h-52 overflow-y-auto"
            >
              <div
                v-for="result in memberSearchResults"
                :key="result.id"
                class="flex items-center gap-2 px-3 py-2 text-sm hover:bg-primary/10 cursor-pointer"
                @click="addMemberFromSearch(result)"
              >
                <div class="flex-1 min-w-0">
                  <p class="font-medium truncate">{{ result.fullName }}</p>
                  <p class="text-xs text-muted truncate">{{ result.email }}</p>
                </div>
                <AppBadge variant="primary" size="sm">{{ result.role }}</AppBadge>
              </div>
              <div v-if="searchingMembers" class="px-3 py-2 text-sm text-muted">
                Searching…
              </div>
            </div>
          </div>
          <button
            class="btn-primary btn-sm"
            :disabled="!pendingMemberIds.length"
            @click="addPendingMembers"
          >
            Add
          </button>
        </div>

        <div v-if="pendingMemberIds.length" class="flex flex-wrap gap-1">
          <span
            v-for="id in pendingMemberIds"
            :key="id"
            class="inline-flex items-center gap-1 rounded bg-primary/10 text-primary text-xs px-2 py-0.5"
          >
            {{ pendingMemberLabel(id) }}
            <button
              class="hover:text-danger leading-none"
              @click="removePendingMember(id)"
            >
              ×
            </button>
          </span>
        </div>

        <div v-if="membersLoading" class="text-sm text-muted py-4 text-center">
          Loading members…
        </div>

        <ul v-else-if="members.length" class="divide-y divide-border dark:divide-border-dark">
          <li
            v-for="m in members"
            :key="m.id"
            class="flex items-center justify-between py-3 text-sm"
          >
            <div class="min-w-0 flex-1">
              <p class="font-medium">{{ m.fullName }}</p>
              <p class="text-xs text-muted">{{ m.email }}</p>
            </div>
            <AppBadge variant="primary" size="sm">{{ m.role?.name ?? "—" }}</AppBadge>
            <button
              class="ml-3 text-xs text-danger hover:underline shrink-0"
              @click="removeExistingMember(m.id)"
            >
              Remove
            </button>
          </li>
        </ul>

        <p v-else class="text-sm text-muted py-4 text-center">
          No members in this group.
        </p>
      </div>
    </div>

    <AppModal
      v-if="groupToDelete"
      :open="!!groupToDelete"
      :title="`Delete ${groupToDelete.name}?`"
      @close="groupToDelete = null"
    >
      <div class="space-y-4">
        <p class="text-sm text-muted">
          This will permanently delete this group and unassign all members.
          This action cannot be undone.
        </p>
        <div>
          <label class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5">
            Type <span class="font-bold">{{ groupToDelete.name }}</span> to confirm
          </label>
          <input
            v-model="deleteConfirmInput"
            type="text"
            class="input"
            :placeholder="groupToDelete.name"
          />
        </div>
      </div>
      <template #footer>
        <button class="btn-ghost btn-sm" @click="groupToDelete = null">
          Cancel
        </button>
        <button
          class="btn-danger btn-sm"
          :disabled="deleteConfirmInput !== groupToDelete.name || deleting"
          @click="confirmDeleteGroup"
        >
          {{ deleting ? "Deleting…" : "Delete Group" }}
        </button>
      </template>
    </AppModal>

    <p v-if="loadError" class="text-sm text-danger">{{ loadError }}</p>
  </div>
</template>

<script setup lang="ts">
import type { Group, Page, User, UserSearchResult } from "~/types"
import { formatDateInTenantTimezone } from "~/composables/timezone"

definePageMeta({ layout: "default", middleware: ["auth"] })

useHead({ title: "Groups — Audita" })

const auth = useAuthStore()
const { success: toastSuccess, error: toastError } = useToast()
const { deleteGroup, fetchGroupMembers, addGroupMembers, removeGroupMembers } = useGroups()
const { searchUsers } = useUserSearch()
const api = useApi()

const page = ref(1)
const pageSize = 20
const loadError = ref("")

const { data, pending, refresh } = await useAsyncData(
  "groups-index-page",
  async () => {
    loadError.value = ""
    try {
      const result = (await api(
        `/api/v1/groups?page=${page.value - 1}&size=${pageSize}`,
      )) as Page<Group>
      console.log("[groups] API response:", new Date().toISOString(), result)
      return result
    } catch (err: unknown) {
      loadError.value = resolveApiErrorMessage(err, "Failed to load groups.")
      return { content: [], totalElements: 0, totalPages: 0, size: pageSize, number: 0 } as Page<Group>
    }
  },
  { watch: [page] },
)

const groups = computed<Group[]>(() => data.value?.content ?? [])
const total = computed(() => data.value?.totalElements ?? groups.value.length)

watch(data, (val) => {
  console.log("[groups] data ref:", new Date().toISOString(), "contentLength:", val?.content?.length, "totalElements:", val?.totalElements, "raw:", val)
}, { immediate: true })

function onPageChange(nextPage: number) {
  page.value = nextPage
}

const columns = computed(() => {
  const base = [
    { key: "name", label: "Group Name" },
    { key: "description", label: "Description" },
    { key: "memberCount", label: "Members" },
    { key: "createdAt", label: "Created At" },
  ]
  if (auth.isAdmin) {
    base.push({ key: "actions", label: "" })
  }
  return base
})

function formatDate(iso: string) {
  return formatDateInTenantTimezone(iso)
}

const expandedGroup = ref<Group | null>(null)
const members = ref<User[]>([])
const membersLoading = ref(false)

const memberSearchQuery = ref("")
const memberSearchOpen = ref(false)
const memberSearchResults = ref<UserSearchResult[]>([])
const searchingMembers = ref(false)
const pendingMemberIds = ref<string[]>([])

function pendingMemberLabel(id: string) {
  const found = memberSearchResults.value.find((r) => r.id === id)
  return found?.fullName ?? id
}

async function toggleMemberPanel(group: Group) {
  if (expandedGroup.value?.id === group.id) {
    expandedGroup.value = null
    return
  }
  expandedGroup.value = group
  pendingMemberIds.value = []
  memberSearchQuery.value = ""
  memberSearchOpen.value = false
  memberSearchResults.value = []
  await loadMembers()
}

async function loadMembers() {
  if (!expandedGroup.value) return
  membersLoading.value = true
  try {
    const pageData = await fetchGroupMembers(expandedGroup.value.id, 1, 100)
    members.value = pageData.content ?? []
  } catch {
    members.value = []
  } finally {
    membersLoading.value = false
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null

async function onMemberSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)

  const q = memberSearchQuery.value.trim()
  if (!q) {
    memberSearchResults.value = []
    memberSearchOpen.value = false
    return
  }

  searchingMembers.value = true
  searchTimer = setTimeout(async () => {
    try {
      const results = await searchUsers(q, 20)
      memberSearchResults.value = results.filter(
        (r) => !pendingMemberIds.value.includes(r.id) && !members.value.some((m) => m.id === r.id),
      )
      memberSearchOpen.value = true
    } finally {
      searchingMembers.value = false
    }
  }, 300)
}

function addMemberFromSearch(result: UserSearchResult) {
  if (!pendingMemberIds.value.includes(result.id)) {
    pendingMemberIds.value.push(result.id)
  }
  memberSearchQuery.value = ""
  memberSearchOpen.value = false
  memberSearchResults.value = []
}

function removePendingMember(id: string) {
  pendingMemberIds.value = pendingMemberIds.value.filter((pid) => pid !== id)
}

async function addPendingMembers() {
  if (!expandedGroup.value || !pendingMemberIds.value.length) return
  try {
    await addGroupMembers(expandedGroup.value.id, [...pendingMemberIds.value])
    toastSuccess("Members added.")
    pendingMemberIds.value = []
    memberSearchQuery.value = ""
    await loadMembers()
    refresh()
  } catch (error: unknown) {
    toastError(resolveApiErrorMessage(error, "Failed to add members."))
  }
}

async function removeExistingMember(userId: string) {
  if (!expandedGroup.value) return
  try {
    await removeGroupMembers(expandedGroup.value.id, [userId])
    toastSuccess("Member removed.")
    await loadMembers()
    refresh()
  } catch (error: unknown) {
    toastError(resolveApiErrorMessage(error, "Failed to remove member."))
  }
}

const groupToDelete = ref<Group | null>(null)
const deleteConfirmInput = ref("")
const deleting = ref(false)

function openDeleteConfirm(group: Group) {
  groupToDelete.value = group
  deleteConfirmInput.value = ""
}

async function confirmDeleteGroup() {
  if (!groupToDelete.value) return
  const target = groupToDelete.value
  deleting.value = true
  try {
    await deleteGroup(target.id)
    toastSuccess("Group deleted.")
    groupToDelete.value = null
    deleteConfirmInput.value = ""
    if (expandedGroup.value?.id === target.id) {
      expandedGroup.value = null
    }
    refresh()
  } catch (error: unknown) {
    toastError(resolveApiErrorMessage(error, "Failed to delete group."))
  } finally {
    deleting.value = false
  }
}
</script>
