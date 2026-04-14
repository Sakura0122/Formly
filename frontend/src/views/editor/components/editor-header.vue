<script setup lang="ts">
import { useEditorStore } from '@/stores/editor'
import { Icon } from '@iconify/vue'
import { storeToRefs } from 'pinia'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const editorStore = useEditorStore()

const { dirty, canUndo, canRedo } = storeToRefs(editorStore)

const statusType = computed(() => (dirty.value ? 'warning' : 'success'))
const statusLabel = computed(() => (dirty.value ? '未保存' : '草稿'))
const previewDisabled = computed(() => !editorStore.table)

// 返回
const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }

  ElMessage.info('当前没有可返回的历史页面')
}

const handlePreview = () => {
  ElMessage.info('预览能力将在后续阶段接入')
}

// 保存
const saveLoading = ref(false)
const handleSave = async () => {
  saveLoading.value = true
  await Promise.resolve()
  saveLoading.value = false
  ElMessage.success('保存事件已输出，等待后续接入真实逻辑')
}

// 预览
const publishLoading = ref(false)
const handlePublish = async () => {
  publishLoading.value = true
  await Promise.resolve()
  publishLoading.value = false
  ElMessage.success('发布事件已输出，等待后续接入真实逻辑')
}
</script>

<template>
  <header
    class="grid gap-4 border-b border-slate-200 bg-white px-4 py-4 lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] lg:items-center lg:px-6"
  >
    <div class="flex min-w-0 items-center gap-3">
      <el-button circle plain @click="handleBack">
        <Icon class="text-lg text-slate-700" icon="solar:alt-arrow-left-linear" />
      </el-button>
      <div class="min-w-0">
        <div class="flex items-center gap-3">
          <h1 class="truncate text-lg font-semibold text-slate-900">Formly 表单编辑器</h1>
          <el-tag :type="statusType" effect="light" round>
            {{ statusLabel }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="flex items-center justify-center gap-2">
      <button
        :disabled="!canUndo"
        class="flex h-8 w-8 items-center justify-center rounded-full transition disabled:cursor-not-allowed"
        type="button"
        @click="editorStore.undo()"
      >
        <Icon
          :class="!canUndo ? 'text-slate-300' : 'cursor-pointer text-slate-700'"
          icon="solar:undo-left-round-bold"
          width="20"
          height="20"
        />
      </button>
      <button
        :disabled="!canRedo"
        class="flex h-8 w-8 items-center justify-center rounded-full transition disabled:cursor-not-allowed"
        type="button"
        @click="editorStore.redo()"
      >
        <Icon
          :class="!canRedo ? 'text-slate-300' : 'cursor-pointer text-slate-700'"
          icon="solar:undo-right-round-bold"
          width="20"
          height="20"
        />
      </button>
    </div>

    <div class="flex flex-wrap items-center justify-start lg:justify-end">
      <el-button :disabled="previewDisabled" plain @click="handlePreview">
        <Icon class="mr-1 text-base" icon="solar:eye-linear" />
        预览
      </el-button>
      <el-button :loading="saveLoading" type="primary" plain @click="handleSave">
        <Icon class="mr-1 text-base" icon="solar:diskette-linear" />
        保存
      </el-button>
      <el-button :loading="publishLoading" type="primary" @click="handlePublish">
        <Icon class="mr-1 text-base" icon="solar:cloud-upload-linear" />
        发布
      </el-button>
    </div>
  </header>
</template>
