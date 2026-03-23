<script setup lang="ts">
import { ref } from 'vue'

import { EDITOR_DEFAULT_TABLE_FORM, EDITOR_TABLE_LIMITS } from '@/constants/editor'
import type { EditorCreateTableForm } from '@/types/editor'

const emit = defineEmits<{
  (e: 'confirm', payload: EditorCreateTableForm): void
}>()

const visible = ref(false)
const mode = ref<'create' | 'rebuild'>('create')
const rows = ref(EDITOR_DEFAULT_TABLE_FORM.rows)
const columns = ref(EDITOR_DEFAULT_TABLE_FORM.columns)

const open = (nextMode: 'create' | 'rebuild') => {
  mode.value = nextMode
  rows.value = EDITOR_DEFAULT_TABLE_FORM.rows
  columns.value = EDITOR_DEFAULT_TABLE_FORM.columns
  visible.value = true
}

const close = () => {
  visible.value = false
}

const handleConfirm = () => {
  emit('confirm', {
    rows: rows.value,
    columns: columns.value,
  })
  close()
}

defineExpose({
  open,
})
</script>

<template>
  <el-dialog v-model="visible" :title="mode === 'create' ? '新建表格' : '重建表格'" width="320px">
    <el-form label-position="top">
      <el-form-item label="行数">
        <el-input-number
          v-model="rows"
          :max="EDITOR_TABLE_LIMITS.maxRows"
          :min="EDITOR_TABLE_LIMITS.minRows"
          :precision="0"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="列数">
        <el-input-number
          v-model="columns"
          :max="EDITOR_TABLE_LIMITS.maxColumns"
          :min="EDITOR_TABLE_LIMITS.minColumns"
          :precision="0"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="flex justify-end gap-3">
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
