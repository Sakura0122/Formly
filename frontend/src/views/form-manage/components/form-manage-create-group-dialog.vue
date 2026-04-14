<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

import { formGroupApi } from '@/api/form-group'
import type { FormEntityId, FormGroupCreateReq } from '@/api/form-group/type'
import { useRequest } from '@/hooks/useRequest'

const emit = defineEmits<{
  (e: 'success', payload: { id: FormEntityId; type: 'group' }): void
}>()

defineOptions({
  name: 'FormManageCreateGroupDialog',
})

const visible = ref(false)
const parentId = ref<FormEntityId | null>(null)
const parentName = ref('')
const name = ref('')
const sort = ref(0)
const { loading, run: createGroup } = useRequest(formGroupApi.create)

const dialogTitle = computed(() => (parentId.value === null ? '新建分组' : `在 ${parentName.value || '当前分组'} 下新建分组`))

const resetForm = () => {
  parentId.value = null
  parentName.value = ''
  name.value = ''
  sort.value = 0
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
  const trimmedName = name.value.trim()

  if (!trimmedName) {
    ElMessage.warning('请输入分组名称')
    return
  }

  const createdId = await createGroup({
    parentId: parentId.value,
    name: trimmedName,
    sort: sort.value,
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
    <div class="space-y-4">
      <div class="space-y-2">
        <p class="text-sm font-medium text-slate-700">分组名称</p>
        <el-input v-model="name" maxlength="100" placeholder="请输入分组名称" @keydown.enter="handleConfirm" />
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
