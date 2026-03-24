<script setup lang="ts">
import { computed } from 'vue'

import { EDITOR_DEFAULT_IMAGE_URL } from '@/constants/editor'
import type { EditorFieldInstance } from '@/types/editor'

const props = defineProps<{
  field: EditorFieldInstance
}>()

const previewValue = computed(() => {
  if (['radio', 'checkbox', 'select'].includes(props.field.type)) {
    return props.field.options[0]?.value ?? ''
  }

  return ''
})

const previewOptions = computed(() => {
  return props.field.options.length
    ? props.field.options
    : [
        {
          label: '选项一',
          value: 'option_1',
        },
      ]
})

const placeholderText = computed(() => {
  return props.field.placeholder || ''
})

const textContent = computed(() => {
  return props.field.textContent || '请输入文字内容'
})

const imageUrl = computed(() => {
  return props.field.imageUrl || EDITOR_DEFAULT_IMAGE_URL
})

const textAlignClass = computed(() => {
  switch (props.field.horizontalAlign) {
    case 'left':
      return 'text-left'
    case 'right':
      return 'text-right'
    default:
      return 'text-center'
  }
})

const imageJustifyClass = computed(() => {
  switch (props.field.horizontalAlign) {
    case 'left':
      return 'justify-start'
    case 'right':
      return 'justify-end'
    default:
      return 'justify-center'
  }
})

</script>

<template>
  <div
    v-if="field.type === 'text'"
    class="min-h-6 wrap-break-word text-xs leading-5 text-slate-700"
    :class="textAlignClass"
  >
    {{ textContent }}
  </div>

  <div v-else-if="field.type === 'image'" class="flex w-full" :class="imageJustifyClass">
    <el-image
      class="block h-16 w-auto max-w-full overflow-hidden rounded object-cover"
      fit="cover"
      :preview-src-list="[]"
      :src="imageUrl"
    />
  </div>

  <el-input
    v-else-if="field.type === 'input'"
    class="w-full min-w-0"
    disabled
    :placeholder="placeholderText"
    size="small"
  />

  <el-input
    v-else-if="field.type === 'textarea'"
    class="w-full min-w-0"
    disabled
    :autosize="{ minRows: 1, maxRows: 2 }"
    :placeholder="placeholderText"
    type="textarea"
  />

  <el-input-number
    v-else-if="field.type === 'number'"
    class="w-full min-w-0"
    controls-position="right"
    disabled
    style="width: 100%"
  />

  <div v-else-if="field.type === 'radio'" class="flex min-w-0 flex-col gap-0.5 py-0.5">
    <div
      v-for="option in previewOptions"
      :key="option.value"
      class="flex min-w-0 items-center gap-1 text-[11px] leading-4"
      :class="option.value === previewValue ? 'text-slate-700' : 'text-slate-400'"
    >
      <span
        class="flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded-full border"
        :class="option.value === previewValue ? 'border-sky-500' : 'border-slate-300'"
      >
        <span
          v-if="option.value === previewValue"
          class="h-1.5 w-1.5 rounded-full bg-sky-500"
        />
      </span>
      <span class="min-w-0 truncate">{{ option.label }}</span>
    </div>
  </div>

  <div v-else-if="field.type === 'checkbox'" class="flex min-w-0 flex-col gap-0.5 py-0.5">
    <div
      v-for="option in previewOptions"
      :key="option.value"
      class="flex min-w-0 items-center gap-1 text-[11px] leading-4"
      :class="option.value === previewValue ? 'text-slate-700' : 'text-slate-400'"
    >
      <span
        class="flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded-sm border"
        :class="option.value === previewValue ? 'border-sky-500 bg-sky-500/10' : 'border-slate-300'"
      >
        <span v-if="option.value === previewValue" class="h-2 w-2 rounded-sm bg-sky-500" />
      </span>
      <span class="min-w-0 truncate">{{ option.label }}</span>
    </div>
  </div>

  <el-select
    v-else-if="field.type === 'select'"
    class="w-full min-w-0"
    disabled
    :placeholder="placeholderText || '请选择'"
    style="width: 100%"
    size="small"
  >
    <el-option
      v-for="option in previewOptions"
      :key="option.value"
      :label="option.label"
      :value="option.value"
    />
  </el-select>

  <el-date-picker
    v-else-if="field.type === 'date'"
    class="w-full min-w-0"
    disabled
    :placeholder="placeholderText || '选择日期'"
    style="width: 100%"
    size="small"
    type="date"
  />

  <el-switch
    v-else-if="field.type === 'switch'"
    disabled
    :model-value="true"
    size="small"
    :active-text="field.switchActiveText"
    :inactive-text="field.switchInactiveText"
  />

  <el-upload v-else action="#" disabled :auto-upload="false" :show-file-list="false">
    <el-button class="w-full min-w-0" disabled plain size="small" type="primary"
      >上传文件</el-button
    >
  </el-upload>
</template>
