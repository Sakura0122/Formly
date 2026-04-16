<script setup lang="ts">
import { formDefinitionApi } from '@/api/form-definition'
import type { FormDefinitionHistoryItem } from '@/api/form-definition/type'
import type { FormEntityId } from '@/api/form-group/type'
import { useRequest } from '@/hooks/useRequest'
import { useEditorStore } from '@/stores/editor'
import EditorCanvas from '@/views/editor/components/editor-canvas.vue'
import { Icon } from '@iconify/vue'
import { computed, ref, watch } from 'vue'

defineOptions({
  name: 'EditorHistoryPanel',
})

const { formId, publishedVersionId } = defineProps<{
  formId: FormEntityId
  publishedVersionId: FormEntityId | null
}>()

const emit = defineEmits<{
  (e: 'exit'): void
}>()

const editorStore = useEditorStore()
const historyList = ref<FormDefinitionHistoryItem[]>([])
const selectedVersionId = ref<FormEntityId | null>(null)

const selectedItem = computed(() => {
  return historyList.value.find((item) => item.id === selectedVersionId.value) ?? null
})

const formatVersionLabel = (versionNo: number) => `V${versionNo}`

const pickHistoryVersion = (preferredVersionId?: FormEntityId | null) => {
  if (!historyList.value.length) {
    selectedVersionId.value = null
    return
  }

  const firstHistoryItem = historyList.value[0]!
  const targetVersionId = preferredVersionId ?? publishedVersionId

  selectedVersionId.value = historyList.value.some((item) => item.id === targetVersionId)
    ? (targetVersionId ?? firstHistoryItem.id)
    : firstHistoryItem.id
}

const { loading, run: getHistoryApi } = useRequest(formDefinitionApi.getHistory)
const loadHistory = async (preferredVersionId?: FormEntityId | null) => {
  if (!formId) {
    historyList.value = []
    selectedVersionId.value = null
    return
  }

  historyList.value = await getHistoryApi(formId)
  pickHistoryVersion(preferredVersionId)
}

const isSameHistorySchema = (targetSchema: FormDefinitionHistoryItem['schema']) => {
  return JSON.stringify(editorStore.buildSchema()) === JSON.stringify(targetSchema)
}

const handleSelect = (versionId: FormEntityId) => {
  selectedVersionId.value = versionId
}

const handleRestore = async () => {
  if (!selectedItem.value) {
    return
  }

  if (isSameHistorySchema(selectedItem.value.schema)) {
    ElMessage.info('当前草稿与所选历史版本一致')
    return
  }

  if (editorStore.dirty) {
    try {
      await ElMessageBox.confirm('还原历史版本会覆盖当前未保存改动，确定继续吗？', '确认还原', {
        type: 'warning',
        confirmButtonText: '确定还原',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
  }

  const restored = editorStore.restoreTable(selectedItem.value.schema)

  if (!restored) {
    ElMessage.info('当前草稿与所选历史版本一致')
    return
  }

  ElMessage.success(`已还原 ${formatVersionLabel(selectedItem.value.versionNo)}`)
  emit('exit')
}

watch(
  () => [formId, publishedVersionId] as const,
  ([nextFormId, nextPublishedVersionId], previousValue) => {
    const [prevFormId, prevPublishedVersionId] = previousValue ?? []

    if (!nextFormId) {
      historyList.value = []
      selectedVersionId.value = null
      return
    }

    if (nextFormId === prevFormId && nextPublishedVersionId === prevPublishedVersionId) {
      return
    }

    loadHistory(nextPublishedVersionId)
  },
  {
    immediate: true,
  },
)
</script>

<template>
  <section v-loading="loading" class="flex w-full h-full min-h-0 gap-4">
    <div class="flex min-h-0 w-88 shrink-0 flex-col overflow-hidden border border-slate-200 bg-white">
      <div class="border-b border-slate-200 px-5 py-4">
        <div class="flex items-center gap-2">
          <Icon class="text-lg text-slate-600" icon="solar:history-bold" />
          <span class="text-sm font-medium text-slate-800">历史版本</span>
        </div>
      </div>

      <el-scrollbar class="min-h-0 flex-1">
        <div v-if="historyList.length" class="px-5 py-6">
          <el-timeline>
            <el-timeline-item
              v-for="item in historyList"
              :key="item.id"
              type="primary"
              hollow
              placement="top"
              :timestamp="item.createdAt"
            >
              <el-card
                class="cursor-pointer transition-colors"
                :style="{
                  backgroundColor: selectedVersionId === item.id ? 'var(--el-color-primary-light-9)' : '',
                }"
                @click="handleSelect(item.id)"
              >
                <div class="flex items-center justify-between gap-3">
                  <h4 class="text-sm font-semibold text-slate-800">
                    {{ formatVersionLabel(item.versionNo) }}
                  </h4>
                  <el-tag v-if="publishedVersionId === item.id" size="small" type="success" effect="light">
                    当前发布
                  </el-tag>
                </div>
                <p class="mt-2 text-xs text-slate-400">
                  <el-tag size="small" class="align-[0]! mr-1">sakura</el-tag>
                  <span>编辑了表单</span>
                </p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div v-else class="flex h-full min-h-80 flex-col items-center justify-center px-6 text-center">
          <Icon class="mb-4 text-slate-300" icon="solar:document-text-linear" width="52" />
          <p class="text-base font-medium text-slate-700">当前暂无历史版本</p>
          <p class="mt-2 text-sm text-slate-400">这里会展示谁编辑了表单</p>
        </div>
      </el-scrollbar>
    </div>

    <div class="flex min-w-0 min-h-0 flex-1 flex-col overflow-hidden border border-slate-200 bg-white">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div class="flex min-w-0 items-center gap-3">
          <span class="text-sm text-slate-500">预览版本</span>
          <el-tag v-if="selectedItem" effect="light">{{ formatVersionLabel(selectedItem.versionNo) }}</el-tag>
          <span v-else class="text-sm text-slate-400">未选择版本</span>
        </div>
        <span class="text-sm text-slate-400">
          {{ selectedItem ? selectedItem.createdAt : '请选择左侧历史版本' }}
        </span>
      </div>

      <div class="min-h-0 flex-1 bg-[#f5f8fc] p-4">
        <el-scrollbar v-if="selectedItem?.schema" class="h-full">
          <div class="flex min-h-full items-start justify-center">
            <div class="w-full min-w-0 bg-white p-4">
              <EditorCanvas :table="selectedItem.schema" readonly />
            </div>
          </div>
        </el-scrollbar>

        <div v-else class="flex h-full min-h-80 flex-col items-center justify-center text-center">
          <Icon class="mb-4 text-slate-300" icon="solar:eye-linear" width="54" />
          <p class="text-base font-medium text-slate-700">选择左侧版本查看预览</p>
          <p class="mt-2 text-sm text-slate-400">历史模式不会改动当前编辑画布，只有点击还原才会覆盖草稿</p>
        </div>
      </div>

      <div class="flex items-center justify-between border-t border-slate-200 bg-white px-4 py-3">
        <el-button plain @click="emit('exit')">退出</el-button>
        <el-button :disabled="!selectedItem" type="primary" @click="handleRestore">还原此版本</el-button>
      </div>
    </div>
  </section>
</template>
