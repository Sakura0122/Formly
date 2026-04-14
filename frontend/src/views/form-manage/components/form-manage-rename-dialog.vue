<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, ref } from 'vue'

import { formDefinitionApi } from '@/api/form-definition'
import type { FormDefinitionUpdateReq } from '@/api/form-definition/type'
import { formGroupApi } from '@/api/form-group'
import type { FormEntityId, FormGroupUpdateReq } from '@/api/form-group/type'
import { useRequest } from '@/hooks/useRequest'

type RenameTarget =
  | {
      id: FormEntityId
      name: string
      parentId: FormEntityId | null
      sort: number | null
      type: 'group'
    }
  | {
      description: string
      groupId: FormEntityId | null
      id: FormEntityId
      name: string
      sort: number | null
      type: 'form'
    }

const emit = defineEmits<{
  (e: 'success', payload: { id: FormEntityId; type: 'group' | 'form' }): void
}>()

defineOptions({
  name: 'FormManageRenameDialog',
})

const visible = ref(false)
const formRef = ref<FormInstance>()
const nodeType = ref<'group' | 'form'>('group')
const form = ref({
  name: '',
})
const target = ref<RenameTarget | null>(null)
const { loading: updateGroupLoading, run: updateGroup } = useRequest(formGroupApi.update)
const { loading: updateFormLoading, run: updateForm } = useRequest(formDefinitionApi.update)

const dialogTitle = computed(() => (nodeType.value === 'group' ? '修改分组名称' : '修改表单名称'))
const loading = computed(() => updateGroupLoading.value || updateFormLoading.value)

const rules = ref<FormRules<typeof form.value>>({
  name: [
    {
      message: '请输入名称',
      required: true,
      trigger: 'blur',
    },
  ],
})

const resetForm = () => {
  nodeType.value = 'group'
  form.value = {
    name: '',
  }
  target.value = null
  formRef.value?.clearValidate()
}

const open = (payload: RenameTarget) => {
  resetForm()
  nodeType.value = payload.type
  form.value.name = payload.name
  target.value = payload
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

  if (!target.value) {
    return
  }

  const trimmedName = form.value.name.trim()

  if (target.value.type === 'group') {
    const payload: FormGroupUpdateReq = {
      name: trimmedName,
      parentId: target.value.parentId,
      sort: target.value.sort ?? 0,
    }
    await updateGroup(target.value.id, payload)
  } else {
    const payload: FormDefinitionUpdateReq = {
      description: target.value.description,
      groupId: target.value.groupId,
      name: trimmedName,
      sort: target.value.sort ?? 0,
    }
    await updateForm(target.value.id, payload)
  }

  const currentTarget = target.value
  close()
  ElMessage.success('名称更新成功')
  emit('success', {
    id: currentTarget.id,
    type: currentTarget.type,
  })
}

defineExpose({
  close,
  open,
})
</script>

<template>
  <el-dialog v-model="visible" :close-on-click-modal="false" :title="dialogTitle" width="420px" @closed="handleClosed">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item :label="nodeType === 'group' ? '分组名称' : '表单名称'" prop="name">
        <el-input v-model="form.name" maxlength="100" placeholder="请输入名称" @keydown.enter="handleConfirm" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>
