<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { formDefinitionApi } from '@/api/form-definition'
import type { FormDefinitionEditorDetail } from '@/api/form-definition/type'
import { useRequest } from '@/hooks/useRequest'
import FormPreviewCanvas from '@/components/form-preview/form-preview-canvas.vue'

defineOptions({
  name: 'FormPreviewPage',
})

const route = useRoute()

const previewDetail = ref<FormDefinitionEditorDetail | null>(null)

const previewTable = computed(() => {
  return previewDetail.value?.schema ?? null
})

const { loading: pageLoading, run: getEditorApi } = useRequest(formDefinitionApi.getEditor)
const loadPreview = async (formId: string) => {
  if (!formId) {
    previewDetail.value = null
    return
  }

  previewDetail.value = await getEditorApi(formId)
}

watch(
  () => route.query.formId,
  (formId) => {
    loadPreview(formId as string)
  },
  {
    immediate: true,
  },
)
</script>

<template>
  <main v-loading="pageLoading" class="min-h-screen bg-white p-8 lg:p-12">
    <div class="flex min-h-[calc(100vh-4rem)] items-start justify-center overflow-auto">
      <div
        v-if="previewTable"
        class="shrink-0 border border-slate-200 bg-white p-8 shadow-[0_10px_40px_rgba(15,23,42,0.06)]"
      >
        <FormPreviewCanvas :table="previewTable" mode="interactive" />
      </div>

      <div v-else class="flex min-h-[calc(100vh-8rem)] items-center justify-center text-sm text-slate-300">
        暂无可预览草稿
      </div>
    </div>
  </main>
</template>
