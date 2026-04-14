<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

import { formDefinitionApi } from '@/api/form-definition'
import type { FormDefinitionCreateReq } from '@/api/form-definition/type'
import type { FormEntityId } from '@/api/form-group/type'
import { useRequest } from '@/hooks/useRequest'

const emit = defineEmits<{
  (e: 'success', payload: { id: FormEntityId; type: 'form' }): void
}>()

defineOptions({
  name: 'FormManageCreateFormDialog',
})

const visible = ref(false)
const groupId = ref<FormEntityId | null>(null)
const groupName = ref('')
const name = ref('')
const formKey = ref('')
const description = ref('')
const sort = ref(0)
const { loading, run: createForm } = useRequest(formDefinitionApi.create)

const dialogTitle = computed(() => (groupId.value === null ? '新建表单' : `在 ${groupName.value || '当前分组'} 下新建表单`))

const resetForm = () => {
  groupId.value = null
  groupName.value = ''
  name.value = ''
  formKey.value = ''
  description.value = ''
  sort.value = 0
}

const open = (payload?: { groupId: FormEntityId | null; groupName?: string }) => {
  resetForm()
  groupId.value = payload?.groupId ?? null
  groupName.value = payload?.groupName ?? ''
  visible.value = true
}

const close = () => {
  visible.value = false
}

const handleClosed = () => {
  resetForm()
}

const handleConfirm = async () => {
  const trimmedName = name.value.trim()
  const trimmedFormKey = formKey.value.trim()

  if (!trimmedName) {
    ElMessage.warning('请输入表单名称')
    return
  }

  if (!trimmedFormKey) {
    ElMessage.warning('请输入表单标识')
    return
  }

  const createdId = await createForm({
    description: description.value.trim(),
    formKey: trimmedFormKey,
    groupId: groupId.value,
    name: trimmedName,
    sort: sort.value,
  })

  close()
  ElMessage.success('表单创建成功')
  emit('success', {
    id: createdId,
    type: 'form',
  })
}

defineExpose({
  close,
  open,
})
</script>

<template>
  <el-dialog v-model="visible" :close-on-click-modal="false" :title="dialogTitle" width="480px" @closed="handleClosed">
    <div class="space-y-4">
      <div class="space-y-2">
        <p class="text-sm font-medium text-slate-700">表单名称</p>
        <el-input v-model="name" maxlength="100" placeholder="请输入表单名称" />
      </div>

      <div class="space-y-2">
        <p class="text-sm font-medium text-slate-700">表单标识</p>
        <el-input v-model="formKey" maxlength="100" placeholder="请输入表单标识" />
      </div>

      <div class="space-y-2">
        <p class="text-sm font-medium text-slate-700">表单描述</p>
        <el-input
          v-model="description"
          :rows="3"
          maxlength="500"
          placeholder="请输入表单描述"
          resize="none"
          show-word-limit
          type="textarea"
        />
      </div>

      <div class="space-y-2">
        <p class="text-sm font-medium text-slate-700">排序值</p>
        <el-input-number v-model="sort" :min="0" class="!w-full" />
      </div>
    </div>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="close">取消</el-button>
        <el-button :loading="loading" type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
