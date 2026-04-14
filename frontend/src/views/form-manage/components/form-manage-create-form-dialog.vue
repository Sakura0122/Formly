<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, ref } from 'vue'

import { formDefinitionApi } from '@/api/form-definition'
import type { FormEntityId } from '@/api/form-group/type'
import { useRequest } from '@/hooks/useRequest'

const emit = defineEmits<{
  (e: 'success', payload: { id: FormEntityId; type: 'form' }): void
}>()

defineOptions({
  name: 'FormManageCreateFormDialog',
})

const visible = ref(false)
const formRef = ref<FormInstance>()
const groupId = ref<FormEntityId | null>(null)
const groupName = ref('')
const form = ref({
  description: '',
  formKey: '',
  name: '',
  sort: 0,
})
const { loading, run: createForm } = useRequest(formDefinitionApi.create)

const dialogTitle = computed(() => (groupId.value === null ? '新建表单' : `在 ${groupName.value || '当前分组'} 下新建表单`))

const rules = ref<FormRules<typeof form.value>>({
  formKey: [
    {
      message: '请输入表单标识',
      required: true,
      trigger: 'blur',
    },
  ],
  name: [
    {
      message: '请输入表单名称',
      required: true,
      trigger: 'blur',
    },
  ],
})

const resetForm = () => {
  groupId.value = null
  groupName.value = ''
  form.value = {
    description: '',
    formKey: '',
    name: '',
    sort: 0,
  }
  formRef.value?.clearValidate()
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
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  const createdId = await createForm({
    description: form.value.description.trim(),
    formKey: form.value.formKey.trim(),
    groupId: groupId.value,
    name: form.value.name.trim(),
    sort: form.value.sort,
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
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="表单名称" prop="name">
        <el-input v-model="form.name" maxlength="100" placeholder="请输入表单名称" />
      </el-form-item>

      <el-form-item label="表单标识" prop="formKey">
        <el-input v-model="form.formKey" maxlength="100" placeholder="请输入表单标识" />
      </el-form-item>

      <el-form-item label="表单描述" prop="description">
        <el-input
          v-model="form.description"
          :rows="3"
          maxlength="500"
          placeholder="请输入表单描述"
          resize="none"
          show-word-limit
          type="textarea"
        />
      </el-form-item>

      <el-form-item label="排序值" prop="sort">
        <el-input-number v-model="form.sort" :min="0" class="!w-full" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>
