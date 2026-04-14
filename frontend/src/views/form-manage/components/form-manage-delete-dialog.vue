<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

import { formDefinitionApi } from '@/api/form-definition'
import { formGroupApi } from '@/api/form-group'
import type { FormCatalogNode, FormEntityId } from '@/api/form-group/type'
import { useRequest } from '@/hooks/useRequest'

type DeleteTarget = Pick<FormCatalogNode, 'id' | 'name' | 'type'>

const emit = defineEmits<{
  (e: 'success', payload: { id: FormEntityId; type: 'group' | 'form' }): void
}>()

defineOptions({
  name: 'FormManageDeleteDialog',
})

const visible = ref(false)
const confirmed = ref(false)
const target = ref<DeleteTarget | null>(null)

const { loading, run: deleteTarget } = useRequest(async (payload: DeleteTarget) => {
  if (payload.type === 'group') {
    await formGroupApi.delete(payload.id)
    return
  }

  await formDefinitionApi.delete(payload.id)
})

const dialogTitle = computed(() => (target.value?.type === 'group' ? '删除分组' : '删除表单'))

const tipText = computed(() => {
  if (target.value?.type === 'group') {
    return '注意：删除后，此分组下所有表单配置和数据都将被删除。请再次确认此分组后续都不再使用，再执行此操作。'
  }

  return '注意：删除后，此表单下所有配置和数据都将被删除。请再次确认此表单后续都不再使用，再执行此操作。'
})

const checkboxLabel = computed(() => {
  if (target.value?.type === 'group') {
    return '我确认删除此分组和所有数据'
  }

  return '我确认删除此表单和所有数据'
})

const resetState = () => {
  confirmed.value = false
  target.value = null
}

const open = (payload: DeleteTarget) => {
  resetState()
  target.value = payload
  visible.value = true
}

const close = () => {
  visible.value = false
}

const handleClosed = () => {
  resetState()
}

const handleConfirm = async () => {
  if (!target.value || !confirmed.value) {
    return
  }

  const currentTarget = target.value
  await deleteTarget(currentTarget)
  close()
  ElMessage.success(currentTarget.type === 'group' ? '分组删除成功' : '表单删除成功')
  emit('success', {
    id: currentTarget.id,
    type: currentTarget.type,
  })
}

defineExpose({
  open,
})
</script>

<template>
  <el-dialog v-model="visible" :close-on-click-modal="false" :title="dialogTitle" width="520px" @closed="handleClosed">
    <div class="pt-2">
      <p class="text-sm text-amber-500 mb-4">{{ tipText }}</p>

      <el-checkbox v-model="confirmed">
        <span class="text-sm font-normal text-slate-600">{{ checkboxLabel }}</span>
      </el-checkbox>
    </div>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button :disabled="!confirmed" :loading="loading" type="danger" @click="handleConfirm">确认</el-button>
    </template>
  </el-dialog>
</template>
