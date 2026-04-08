<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed } from 'vue'

import { EDITOR_HEADER_ACTION_OPTIONS } from '@/constants/editor'
import type { EditorHeaderActionKey } from '@/types/editor'

const props = withDefaults(
  defineProps<{
    title: string
    statusText?: string
    dirty?: boolean
    undoDisabled?: boolean
    redoDisabled?: boolean
    previewDisabled?: boolean
    saveLoading?: boolean
    publishLoading?: boolean
  }>(),
  {
    statusText: '草稿',
    dirty: false,
    undoDisabled: true,
    redoDisabled: true,
    previewDisabled: false,
    saveLoading: false,
    publishLoading: false,
  },
)

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'undo'): void
  (e: 'redo'): void
  (e: 'preview'): void
  (e: 'save'): void
  (e: 'publish'): void
  (e: 'more-action', action: EditorHeaderActionKey): void
}>()

const statusType = computed(() => (props.dirty ? 'warning' : 'success'))
const statusLabel = computed(() => (props.dirty ? '未保存' : props.statusText))

const handleMoreAction = (action: string | number | object) => {
  emit('more-action', action as EditorHeaderActionKey)
}
</script>

<template>
  <header
    class="grid gap-4 border-b border-slate-200 bg-white px-4 py-4 lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] lg:items-center lg:px-6"
  >
    <div class="flex min-w-0 items-center gap-3">
      <el-button circle plain @click="emit('back')">
        <Icon class="text-lg text-slate-700" icon="solar:alt-arrow-left-linear" />
      </el-button>
      <div class="min-w-0">
        <div class="flex items-center gap-3">
          <h1 class="truncate text-lg font-semibold text-slate-900">{{ title }}</h1>
          <el-tag :type="statusType" effect="light" round>
            {{ statusLabel }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-center gap-2">
      <button
        :disabled="undoDisabled"
        class="flex h-8 w-8 items-center justify-center rounded-full transition disabled:cursor-not-allowed"
        type="button"
        @click="emit('undo')"
      >
        <Icon
          :class="undoDisabled ? 'text-slate-300' : 'cursor-pointer text-slate-700'"
          icon="solar:undo-left-round-bold"
          width="20"
          height="20"
        />
      </button>
      <button
        :disabled="redoDisabled"
        class="flex h-8 w-8 items-center justify-center rounded-full transition disabled:cursor-not-allowed"
        type="button"
        @click="emit('redo')"
      >
        <Icon
          :class="redoDisabled ? 'text-slate-300' : 'cursor-pointer text-slate-700'"
          icon="solar:undo-right-round-bold"
          width="20"
          height="20"
        />
      </button>
    </div>

    <div class="flex flex-wrap items-center justify-start lg:justify-end">
      <el-button :disabled="previewDisabled" plain @click="emit('preview')">
        <Icon class="mr-1 text-base" icon="solar:eye-linear" />
        预览
      </el-button>
      <el-button :loading="saveLoading" type="primary" plain @click="emit('save')">
        <Icon class="mr-1 text-base" icon="solar:diskette-linear" />
        保存
      </el-button>
      <el-button :loading="publishLoading" type="primary" @click="emit('publish')">
        <Icon class="mr-1 text-base" icon="solar:cloud-upload-linear" />
        发布
      </el-button>
      <el-dropdown trigger="click" @command="handleMoreAction" class="ml-3">
        <el-button plain>
          <Icon class="mr-1 text-base" icon="solar:menu-dots-linear" />
          更多
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-for="action in EDITOR_HEADER_ACTION_OPTIONS" :key="action.key" :command="action.key">
              <div class="flex items-center gap-2">
                <Icon class="text-base text-slate-500" :icon="action.icon" />
                <span>{{ action.label }}</span>
              </div>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>
