<script setup lang="ts">
import { computed, ref } from 'vue'

import { formDefinitionApi } from '@/api/form-definition'
import type { FormDefinitionUpdateReq } from '@/api/form-definition/type'
import type { FormEntityId } from '@/api/form-group/type'
import { useRequest } from '@/hooks/useRequest'

const ROOT_GROUP_VALUE = '__root__'

type GroupOption = {
  id: FormEntityId
  label: string
}

type MoveTarget = {
  description: string
  groupId: FormEntityId | null
  id: FormEntityId
  name: string
  sort: number | null
}

const props = defineProps<{
  options: GroupOption[]
}>()

const emit = defineEmits<{
  (e: 'success', payload: { id: FormEntityId; type: 'form' }): void
}>()

defineOptions({
  name: 'FormManageMoveFormDialog',
})

const visible = ref(false)
const selectedGroupId = ref<FormEntityId>(ROOT_GROUP_VALUE)
const target = ref<MoveTarget | null>(null)
const { loading, run: updateForm } = useRequest(formDefinitionApi.update)

const allOptions = computed(() => [{ id: ROOT_GROUP_VALUE as FormEntityId, label: '根目录' }, ...props.options])

const open = (payload: MoveTarget) => {
  target.value = payload
  selectedGroupId.value = payload.groupId ?? ROOT_GROUP_VALUE
  visible.value = true
}

const close = () => {
  visible.value = false
}

const handleConfirm = async () => {
  if (!target.value) {
    return
  }

  const payload: FormDefinitionUpdateReq = {
    description: target.value.description,
    groupId: selectedGroupId.value === ROOT_GROUP_VALUE ? null : selectedGroupId.value,
    name: target.value.name,
    sort: target.value.sort ?? 0,
  }

  await updateForm(target.value.id, payload)

  const currentTarget = target.value
  close()
  ElMessage.success('表单移动成功')
  emit('success', {
    id: currentTarget.id,
    type: 'form',
  })
}

defineExpose({
  close,
  open,
})
</script>

<template>
  <el-dialog v-model="visible" :close-on-click-modal="false" title="移动表单" width="420px">
    <div class="space-y-2">
      <p class="text-sm font-medium text-slate-700">目标分组</p>
      <el-select v-model="selectedGroupId" class="w-full" placeholder="请选择目标分组">
        <el-option v-for="option in allOptions" :key="option.id" :label="option.label" :value="option.id" />
      </el-select>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="close">取消</el-button>
        <el-button :loading="loading" type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
