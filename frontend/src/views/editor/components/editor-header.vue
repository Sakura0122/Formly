<script setup lang="ts">
import { formVersionApi } from '@/api/form-version'
import { useRequest } from '@/hooks/useRequest'
import { useEditorStore } from '@/stores/editor'
import { Icon } from '@iconify/vue'
import { storeToRefs } from 'pinia'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

type MoreAction = 'history'

const emit = defineEmits<{
  (e: 'open-history'): void
}>()

const router = useRouter()
const editorStore = useEditorStore()

const { dirty, canUndo, canRedo, currentFormId, publishedVersionId, hasSavedDraft, hasUnpublishedDraft, formName } =
  storeToRefs(editorStore)

const statusType = computed(() => {
  if (dirty.value) {
    return 'warning'
  }

  if (hasUnpublishedDraft.value) {
    return 'warning'
  }

  if (publishedVersionId.value && !hasUnpublishedDraft.value) {
    return 'success'
  }

  return 'info'
})

const statusLabel = computed(() => {
  if (dirty.value) {
    return '未保存'
  }

  if (hasUnpublishedDraft.value) {
    return '有未发布改动'
  }

  if (publishedVersionId.value && !hasUnpublishedDraft.value) {
    return '已发布'
  }

  if (editorStore.table) {
    return '草稿已保存'
  }

  return '未保存'
})

const previewDisabled = computed(() => !currentFormId.value || !hasSavedDraft.value)
const saveDisabled = computed(() => !currentFormId.value || !editorStore.table || !dirty.value)
const publishDisabled = computed(() => !currentFormId.value || !editorStore.table)
const historyDisabled = computed(() => !currentFormId.value || !publishedVersionId.value)
const formatVersionLabel = (versionNo: number) => `V${versionNo}`

// 返回
const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }

  ElMessage.info('当前没有可返回的历史页面')
}

const handlePreview = () => {
  if (!currentFormId.value || !hasSavedDraft.value) {
    return
  }

  const previewRoute = router.resolve({
    path: '/form-preview',
    query: {
      formId: String(currentFormId.value),
    },
  })
  const previewWindow = window.open(previewRoute.href, '_blank', 'noopener')

  if (!previewWindow) {
    ElMessage.warning('预览窗口被拦截，请允许浏览器打开新窗口')
  }
}

const handleMoreAction = (command: MoreAction) => {
  if (command !== 'history' || historyDisabled.value) {
    return
  }

  emit('open-history')
}

// 保存
const { loading: saveLoading, run: saveFormVersionApi } = useRequest(formVersionApi.save)
const handleSave = async () => {
  if (!currentFormId.value || !editorStore.table) {
    ElMessage.warning('当前表单还不能保存')
    return
  }

  const res = await saveFormVersionApi(currentFormId.value, {
    schemaJson: editorStore.buildSchema(),
  })

  editorStore.markPersisted({
    publishedVersionId: res.publishedVersionId,
    hasUnpublishedDraft: res.hasUnpublishedDraft,
  })
  ElMessage.success('草稿已保存')
}

// 保存并发布
const { loading: publishLoading, run: publishFormVersionApi } = useRequest(formVersionApi.publish)
const handlePublish = async () => {
  if (!currentFormId.value || !editorStore.table) {
    ElMessage.warning('当前表单还不能发布')
    return
  }

  const res = await publishFormVersionApi(currentFormId.value, {
    schemaJson: editorStore.buildSchema(),
  })

  editorStore.markPersisted({
    publishedVersionId: res.publishedVersionId,
    hasUnpublishedDraft: res.hasUnpublishedDraft,
  })

  if (res.alreadyPublished) {
    ElMessage.info('当前已是最新发布版本')
    return
  }

  const publishedMessage =
    res.versionNo === null ? '已保存并发布' : `已保存并发布 ${formatVersionLabel(res.versionNo)}`
  ElMessage.success(publishedMessage)
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
          <h1 class="truncate text-lg font-semibold text-slate-900">{{ formName }}</h1>
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

    <div class="flex flex-wrap items-center justify-start gap-2 lg:justify-end">
      <el-dropdown :disabled="historyDisabled" placement="bottom-end" trigger="hover" @command="handleMoreAction">
        <div class="editor-header-more-trigger cursor-pointer text-slate-500 hover:text-slate-700">
          <Icon class="text-base" icon="solar:menu-dots-bold" />
        </div>

        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="history">历史版本</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-button :disabled="previewDisabled" plain @click="handlePreview">
        <Icon class="mr-1 text-base" icon="solar:eye-linear" />
        预览
      </el-button>
      <el-button :disabled="saveDisabled" :loading="saveLoading" type="primary" plain @click="handleSave">
        <Icon class="mr-1 text-base" icon="solar:diskette-linear" />
        保存
      </el-button>
      <el-button :disabled="publishDisabled" :loading="publishLoading" type="primary" @click="handlePublish">
        <Icon class="mr-1 text-base" icon="solar:cloud-upload-linear" />
        保存并发布
      </el-button>
    </div>
  </header>
</template>

<style scoped>
.editor-header-more-trigger {
  outline: none;
  box-shadow: none;
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.editor-header-more-trigger:focus,
.editor-header-more-trigger:focus-visible,
.editor-header-more-trigger:active {
  outline: none;
  box-shadow: none;
}
</style>
