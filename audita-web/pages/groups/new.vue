<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <p
          class="text-xs font-semibold uppercase tracking-[0.16em] text-primary/70 mb-1"
        >
          IAM Framework
        </p>
        <h1 class="text-3xl font-bold tracking-tight">New Group</h1>
        <p class="text-sm text-muted mt-1">
          Create a group and optionally assign members.
        </p>
      </div>
      <button class="btn-ghost btn-md" @click="navigateTo('/groups')">
        Cancel
      </button>
    </div>

    <div class="card shadow-card-hover overflow-hidden">
      <div class="px-6 py-4 border-b border-border dark:border-border-dark">
        <div class="flex items-center gap-3">
          <div
            v-for="(step, i) in steps"
            :key="i"
            class="flex items-center gap-2"
          >
            <div
              class="flex items-center justify-center w-8 h-8 rounded-full text-sm font-semibold transition-colors"
              :class="
                currentStep > i
                  ? 'bg-primary text-white'
                  : currentStep === i
                    ? 'bg-primary/20 text-primary ring-2 ring-primary'
                    : 'bg-gray-100 dark:bg-slate-700 text-muted'
              "
            >
              <template v-if="currentStep > i">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </template>
              <template v-else>{{ i + 1 }}</template>
            </div>
            <span
              class="text-sm font-medium"
              :class="currentStep >= i ? 'text-on-surface dark:text-gray-100' : 'text-muted'"
            >
              {{ step }}
            </span>
            <div
              v-if="i < steps.length - 1"
              class="w-12 h-0.5 rounded"
              :class="currentStep > i ? 'bg-primary' : 'bg-border dark:bg-border-dark'"
            />
          </div>
        </div>
      </div>

      <div class="p-6 min-h-[300px]">
        <div v-if="errorMessage" class="mb-4 rounded-md border border-danger-border bg-danger-light px-4 py-3 text-sm text-danger">
          {{ errorMessage }}
        </div>

        <div v-if="currentStep === 0">
          <div class="space-y-5">
            <div>
              <label
                for="group-name"
                class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5"
              >
                Group Name <span class="text-danger">*</span>
              </label>
              <input
                id="group-name"
                v-model="form.name"
                type="text"
                class="input"
                :class="{ 'input-error': nameError }"
                maxlength="255"
                placeholder="Enter group name"
                @blur="nameError = !form.name.trim()"
              />
              <p v-if="nameError" class="field-error">Group name is required.</p>
            </div>

            <div>
              <label
                for="group-description"
                class="block text-xs font-semibold text-muted uppercase tracking-wide mb-1.5"
              >
                Description
              </label>
              <textarea
                id="group-description"
                v-model="form.description"
                class="input min-h-[100px]"
                placeholder="Optional description"
              />
            </div>
          </div>
        </div>

        <div v-if="currentStep === 1">
          <div class="space-y-4">
            <p class="text-sm text-muted">
              Search and select users to add to this group. You can skip this step.
            </p>

            <div class="relative">
              <div
                class="input flex flex-wrap gap-1 min-h-[2.5rem] cursor-text"
                @click="memberSearchInputRef?.focus()"
              >
                <span
                  v-for="user in selectedMembers"
                  :key="user.id"
                  class="inline-flex items-center gap-1 rounded bg-primary/10 text-primary text-xs px-2 py-0.5"
                >
                  {{ user.fullName }}
                  <button
                    class="hover:text-danger leading-none"
                    @click.stop="removeSelectedMember(user.id)"
                    :aria-label="`Remove ${user.fullName}`"
                  >
                    ×
                  </button>
                </span>
                <input
                  ref="memberSearchInputRef"
                  v-model="memberSearchText"
                  class="flex-1 min-w-[10rem] bg-transparent outline-none text-sm"
                  placeholder="Search users…"
                  @input="onMemberSearchInput"
                  @focus="memberSearchDropdownOpen = !!memberSearchText"
                />
              </div>

              <div
                v-if="memberSearchDropdownOpen && memberSearchResults.length"
                class="absolute z-30 mt-1 w-full bg-white dark:bg-slate-800 border border-border dark:border-border-dark rounded-lg shadow-lg max-h-64 overflow-y-auto"
              >
                <div
                  v-for="result in memberSearchResults"
                  :key="result.id"
                  class="flex items-center gap-3 px-3 py-2.5 text-sm hover:bg-primary/10 cursor-pointer"
                  @click="selectMember(result)"
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
          </div>
        </div>

        <div v-if="currentStep === 2">
          <div class="card border border-border dark:border-border-dark p-5 space-y-4">
            <h3 class="text-sm font-semibold uppercase tracking-wide text-muted">Summary</h3>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <p class="text-xs text-muted uppercase tracking-wide">Group Name</p>
                <p class="text-sm font-semibold mt-0.5">{{ form.name }}</p>
              </div>
              <div>
                <p class="text-xs text-muted uppercase tracking-wide">Description</p>
                <p class="text-sm mt-0.5">{{ form.description || "No description" }}</p>
              </div>
              <div class="sm:col-span-2">
                <p class="text-xs text-muted uppercase tracking-wide">Members</p>
                <template v-if="selectedMembers.length">
                  <div class="flex flex-wrap gap-1 mt-0.5">
                    <span
                      v-for="user in selectedMembers"
                      :key="user.id"
                      class="inline-flex items-center gap-1 rounded bg-primary/10 text-primary text-xs px-2 py-0.5"
                    >
                      {{ user.fullName }}
                    </span>
                  </div>
                </template>
                <p v-else class="text-sm text-muted mt-0.5">No members</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      class="fixed bottom-0 left-0 right-0 bg-white dark:bg-slate-950 border-t border-border dark:border-border-dark z-30 md:left-14"
    >
      <div class="max-w-4xl mx-auto flex items-center justify-between px-6 py-4">
        <div class="flex items-center gap-2 text-sm text-muted">
          <span>Step {{ currentStep + 1 }} of {{ steps.length }}</span>
          <span class="text-border dark:text-border-dark">|</span>
          <span>{{ steps[currentStep] }}</span>
        </div>
        <div class="flex items-center gap-2">
          <button
            v-if="currentStep > 0"
            class="btn-ghost btn-md"
            @click="currentStep--"
          >
            Back
          </button>
          <button
            v-if="currentStep === 0"
            class="btn-primary btn-md"
            :disabled="!form.name.trim()"
            @click="currentStep++"
          >
            Continue
          </button>
          <button
            v-if="currentStep === 1"
            class="btn-ghost btn-md"
            @click="currentStep++"
          >
            Skip
          </button>
          <button
            v-if="currentStep === 1"
            class="btn-primary btn-md"
            @click="currentStep++"
          >
            Continue
          </button>
          <button
            v-if="currentStep === 2"
            class="btn-primary btn-md"
            :disabled="creating"
            @click="handleCreate"
          >
            {{ creating ? "Creating…" : "Create Group" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { UserSearchResult } from "~/types"

definePageMeta({ middleware: ["auth", "admin-only"] })

useHead({ title: "New Group — Audita" })

const { createGroup } = useGroups()
const { searchUsers } = useUserSearch()
const { success: toastSuccess, error: toastError } = useToast()

const steps = ["Details", "Members", "Review"]
const currentStep = ref(0)

const form = reactive({
  name: "",
  description: "",
})

const nameError = ref(false)
const creating = ref(false)
const errorMessage = ref("")

const selectedMembers = ref<UserSearchResult[]>([])
const memberSearchText = ref("")
const memberSearchResults = ref<UserSearchResult[]>([])
const memberSearchDropdownOpen = ref(false)
const searchingMembers = ref(false)
const memberSearchInputRef = ref<HTMLInputElement | null>(null)

let searchTimer: ReturnType<typeof setTimeout> | null = null

async function onMemberSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)

  const q = memberSearchText.value.trim()
  if (!q) {
    memberSearchResults.value = []
    memberSearchDropdownOpen.value = false
    return
  }

  searchingMembers.value = true
  searchTimer = setTimeout(async () => {
    try {
      const results = await searchUsers(q, 20)
      memberSearchResults.value = results.filter(
        (r) => !selectedMembers.value.some((m) => m.id === r.id),
      )
      memberSearchDropdownOpen.value = true
    } finally {
      searchingMembers.value = false
    }
  }, 300)
}

function selectMember(result: UserSearchResult) {
  if (!selectedMembers.value.some((m) => m.id === result.id)) {
    selectedMembers.value.push(result)
  }
  memberSearchText.value = ""
  memberSearchResults.value = []
  memberSearchDropdownOpen.value = false
}

function removeSelectedMember(id: string) {
  selectedMembers.value = selectedMembers.value.filter((m) => m.id !== id)
}

async function handleCreate() {
  nameError.value = !form.name.trim()
  if (nameError.value) {
    currentStep.value = 0
    return
  }

  creating.value = true
  errorMessage.value = ""
  try {
    const payload: { name: string; description?: string; memberIds?: string[] } = {
      name: form.name.trim(),
    }
    if (form.description.trim()) {
      payload.description = form.description.trim()
    }
    if (selectedMembers.value.length) {
      payload.memberIds = selectedMembers.value.map((m) => m.id)
    }

    await createGroup(payload)
    toastSuccess("Group created.")
    await navigateTo("/groups")
  } catch (error: unknown) {
    errorMessage.value = resolveApiErrorMessage(error, "Failed to create group.")
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  document.addEventListener("click", (e) => {
    const target = e.target as Node
    const input = memberSearchInputRef.value
    if (!input || !input.parentElement?.parentElement) return
    if (!input.parentElement.parentElement.contains(target)) {
      memberSearchDropdownOpen.value = false
    }
  })
})
</script>
