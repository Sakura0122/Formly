<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, ref } from 'vue'

import { formGroupApi } from '@/api/form-group'
import type { FormEntityId } from '@/api/form-group/type'
import { useRequest } from '@/hooks/useRequest'

const emit = defineEmits<{
  (e: 'success', payload: { id: FormEntityId; type: 'group' }): void
}>()

defineOptions({
  name: 'FormManageCreateGroupDialog',
})

const visible = ref(false)
const formRef = ref<FormInstance>()
const parentId = ref<FormEntityId | null>(null)
const parentName = ref('')
const form = ref({
  name: '',
  sort: 0,
})
const { loading, run: createGroup } = useRequest(formGroupApi.create)

const dialogTitle = computed(() =>
  parentId.value === null ? '新建分组' : `在 ${parentName.value || '当前分组'} 下新建分组`,
)

const rules = ref<FormRules<typeof form.value>>({
  name: [
    {
      message: '请输入分组名称',
      required: true,
      trigger: 'blur',
    },
  ],
})

const resetForm = () => {
  parentId.value = null
  parentName.value = ''
  form.value = {
    name: '',
    sort: 0,
  }
  formRef.value?.clearValidate()
}

const open = (payload?: { parentId: FormEntityId | null; parentName?: string }) => {
  resetForm()
  parentId.value = payload?.parentId ?? null
  parentName.value = payload?.parentName ?? ''
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

  const createdId = await createGroup({
    parentId: parentId.value,
    name: form.value.name.trim(),
    sort: form.value.sort,
  })

  close()
  ElMessage.success('分组创建成功')
  emit('success', {
    id: createdId,
    type: 'group',
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
      <el-form-item label="分组名称" prop="name">
        <el-input v-model="form.name" maxlength="100" placeholder="请输入分组名称" @keydown.enter="handleConfirm" />
      </el-form-item>

      <el-form-item label="排序值" prop="sort">
        <el-input-number v-model="form.sort" :min="0" class="w-full!" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>
