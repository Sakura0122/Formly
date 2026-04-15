<script setup lang="ts">
import { Icon } from '@iconify/vue'
import type { UploadFile, UploadUserFile } from 'element-plus'
import { computed } from 'vue'

import type { EditorFormPreviewMode, EditorFieldInstance } from '@/types/editor'

const props = withDefaults(
  defineProps<{
    field: EditorFieldInstance
    mode?: EditorFormPreviewMode
    modelValue?: unknown
  }>(),
  {
    mode: 'readonly',
    modelValue: undefined,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: unknown): void
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

const isReadonly = computed(() => props.mode === 'readonly')
const isInteractive = computed(() => props.mode === 'interactive')

const inlineCellPlaceholderText = computed(() => {
  return props.field.placeholder || '/'
})

const selectPlaceholderText = computed(() => {
  return props.field.placeholder || '请选择'
})

const dateDisplayText = computed(() => {
  return props.field.placeholder || 'YYYY-MM-DD'
})

const textContent = computed(() => {
  return props.field.textContent || '固定文字内容'
})

const imageUrl = computed(() => {
  return props.field.imageUrl.trim()
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

const justifyClass = computed(() => {
  switch (props.field.horizontalAlign) {
    case 'left':
      return 'justify-start'
    case 'right':
      return 'justify-end'
    default:
      return 'justify-center'
  }
})

const previewControlAlignClass = computed(() => {
  switch (props.field.horizontalAlign) {
    case 'left':
      return 'preview-cell-control-left'
    case 'right':
      return 'preview-cell-control-right'
    default:
      return 'preview-cell-control-center'
  }
})

const previewControlStyle = computed<Record<string, string>>(() => {
  return {
    '--el-input-bg-color': 'transparent',
    '--el-input-border-color': 'transparent',
    '--el-input-hover-border-color': 'transparent',
    '--el-input-focus-border-color': 'transparent',
    '--el-fill-color-blank': 'transparent',
    '--el-select-disabled-border': 'transparent',
  }
})

const inputStyle = computed<Record<string, string>>(() => {
  return {
    textAlign: props.field.horizontalAlign,
  }
})

const textInputValue = computed({
  get: () => (typeof props.modelValue === 'string' ? props.modelValue : ''),
  set: (value: string) => {
    emit('update:modelValue', value)
  },
})

const checkboxValue = computed({
  get: () => (Array.isArray(props.modelValue) ? props.modelValue : []),
  set: (value: string[]) => {
    emit('update:modelValue', value)
  },
})

const switchValue = computed({
  get: () => Boolean(props.modelValue),
  set: (value: boolean) => {
    emit('update:modelValue', value)
  },
})

const uploadFileList = computed(() => {
  return Array.isArray(props.modelValue) ? (props.modelValue as UploadUserFile[]) : []
})

const handleUploadChange = (_file: UploadFile, fileList: UploadUserFile[]) => {
  emit('update:modelValue', fileList)
}

const handleUploadRemove = (_file: UploadFile, fileList: UploadUserFile[]) => {
  emit('update:modelValue', fileList)
}
</script>

<template>
  <div v-if="field.type === 'text'" class="flex min-h-6 w-full min-w-0 items-center px-2" :class="justifyClass">
    <div class="w-full wrap-break-word text-xs leading-5 text-slate-700" :class="textAlignClass">
      {{ textContent }}
    </div>
  </div>

  <div v-else-if="field.type === 'image'" class="flex w-full" :class="justifyClass">
    <el-image
      v-if="imageUrl"
      class="block h-16 w-auto max-w-full overflow-hidden rounded object-cover"
      fit="cover"
      :preview-src-list="[]"
      :src="imageUrl"
    />
    <div
      v-else
      class="flex h-16 w-20 items-center justify-center rounded border border-slate-200 bg-slate-50 text-slate-300"
    >
      <Icon class="text-3xl" icon="ep:picture-filled" />
    </div>
  </div>

  <div v-else-if="field.type === 'textbox'">
    <div v-if="isReadonly" class="flex min-h-6 w-full min-w-0 items-center px-2" :class="justifyClass">
      <div class="w-full wrap-break-word text-xs leading-5 text-slate-400" :class="textAlignClass">
        {{ inlineCellPlaceholderText }}
      </div>
    </div>
    <el-input
      v-else
      v-model="textInputValue"
      class="preview-cell-control w-full min-w-0"
      :class="isInteractive ? previewControlAlignClass : ''"
      :input-style="inputStyle"
      :placeholder="inlineCellPlaceholderText"
      size="small"
      :style="isInteractive ? previewControlStyle : undefined"
    />
  </div>

  <div v-else-if="field.type === 'number'">
    <div
      v-if="isReadonly"
      class="flex min-h-6 w-full min-w-0 items-center px-2 tabular-nums"
      :class="justifyClass"
    >
      <div class="w-full wrap-break-word text-xs leading-5 text-slate-400" :class="textAlignClass">
        {{ inlineCellPlaceholderText }}
      </div>
    </div>
    <el-input
      v-else
      v-model="textInputValue"
      class="preview-cell-control w-full min-w-0"
      :class="isInteractive ? previewControlAlignClass : ''"
      :input-style="inputStyle"
      :placeholder="inlineCellPlaceholderText"
      size="small"
      :style="isInteractive ? previewControlStyle : undefined"
      type="number"
    />
  </div>

  <div v-else-if="field.type === 'radio'" class="flex w-full" :class="justifyClass">
    <div v-if="isReadonly" class="flex min-w-0 max-w-full flex-col gap-0.5 py-0">
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
          <span v-if="option.value === previewValue" class="h-1.5 w-1.5 rounded-full bg-sky-500" />
        </span>
        <span class="min-w-0 truncate">{{ option.label }}</span>
      </div>
    </div>
    <el-radio-group
      v-else
      v-model="textInputValue"
      class="preview-choice-group flex min-w-0 max-w-full flex-col items-start gap-1 py-0"
    >
      <el-radio v-for="option in previewOptions" :key="option.value" :value="option.value">
        {{ option.label }}
      </el-radio>
    </el-radio-group>
  </div>

  <div v-else-if="field.type === 'checkbox'" class="flex w-full" :class="justifyClass">
    <div v-if="isReadonly" class="flex min-w-0 max-w-full flex-col gap-0.5 py-0">
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
    <el-checkbox-group
      v-else
      v-model="checkboxValue"
      class="preview-choice-group flex min-w-0 max-w-full flex-col items-start gap-1 py-0"
    >
      <el-checkbox v-for="option in previewOptions" :key="option.value" :value="option.value">
        {{ option.label }}
      </el-checkbox>
    </el-checkbox-group>
  </div>

  <div v-else-if="field.type === 'select'" class="preview-select-root flex h-full w-full items-center">
    <el-select
      v-model="textInputValue"
      class="w-full min-w-0"
      :class="isInteractive ? ['preview-cell-control', 'preview-select-control', previewControlAlignClass] : ''"
      :disabled="isReadonly"
      :placeholder="isReadonly ? selectPlaceholderText : inlineCellPlaceholderText"
      size="small"
      style="width: 100%"
      :style="isInteractive ? previewControlStyle : undefined"
    >
      <el-option v-for="option in previewOptions" :key="option.value" :label="option.label" :value="option.value" />
    </el-select>
  </div>

  <div v-else-if="field.type === 'date'">
    <div v-if="isReadonly" class="flex h-6 w-full min-w-0 items-center px-2">
      <div class="min-w-0 w-full whitespace-nowrap text-xs leading-5 text-slate-700" :class="textAlignClass">
        {{ dateDisplayText }}
      </div>
    </div>
    <el-date-picker
      v-else
      v-model="textInputValue"
      class="w-full"
      format="YYYY-MM-DD"
      :class="isInteractive ? ['preview-cell-control', previewControlAlignClass] : ''"
      :placeholder="inlineCellPlaceholderText"
      size="small"
      :style="isInteractive ? previewControlStyle : undefined"
      type="date"
      value-format="YYYY-MM-DD"
    />
  </div>

  <div v-else-if="field.type === 'switch'" class="flex w-full" :class="justifyClass">
    <el-switch
      v-model="switchValue"
      :disabled="isReadonly"
      size="small"
      :active-text="field.switchActiveText"
      :inactive-text="field.switchInactiveText"
    />
  </div>

  <div v-else class="flex w-full items-center" :class="justifyClass">
    <el-upload
      class="preview-upload-control"
      action="#"
      :auto-upload="false"
      :disabled="isReadonly"
      :file-list="uploadFileList"
      :on-change="handleUploadChange"
      :on-remove="handleUploadRemove"
      list-type="text"
    >
      <el-button class="min-w-0" :disabled="isReadonly" plain size="small" type="primary">上传文件</el-button>
    </el-upload>
  </div>
</template>

<style scoped>
.preview-cell-control :deep(.el-input__wrapper),
.preview-cell-control :deep(.el-select__wrapper),
.preview-cell-control :deep(.el-date-editor.el-input__wrapper),
.preview-cell-control :deep(.el-date-editor .el-input__wrapper) {
  box-shadow: none;
  background-color: transparent;
  border-radius: 0;
  min-height: 24px;
  padding-inline: 0;
}

.preview-cell-control :deep(.el-input__wrapper:hover),
.preview-cell-control :deep(.el-input__wrapper.is-focus),
.preview-cell-control :deep(.el-select__wrapper.is-hovering:not(.is-focused)),
.preview-cell-control :deep(.el-select__wrapper.is-focused),
.preview-cell-control :deep(.el-date-editor.el-input__wrapper:hover),
.preview-cell-control :deep(.el-date-editor.is-focus .el-input__wrapper) {
  box-shadow: none;
}

.preview-cell-control :deep(.el-input__inner),
.preview-cell-control :deep(.el-select__selected-item),
.preview-cell-control :deep(.el-select__placeholder),
.preview-cell-control :deep(.el-date-editor .el-input__inner) {
  color: #334155;
}

.preview-cell-control-left :deep(.el-input__inner),
.preview-cell-control-left :deep(.el-date-editor .el-input__inner) {
  text-align: left;
}

.preview-cell-control-center :deep(.el-input__inner),
.preview-cell-control-center :deep(.el-date-editor .el-input__inner) {
  text-align: center;
}

.preview-cell-control-right :deep(.el-input__inner),
.preview-cell-control-right :deep(.el-date-editor .el-input__inner) {
  text-align: right;
}

.preview-cell-control-left :deep(.el-select__selection) {
  justify-content: flex-start;
}

.preview-cell-control-center :deep(.el-select__selection) {
  justify-content: center;
}

.preview-cell-control-right :deep(.el-select__selection) {
  justify-content: flex-end;
}

.preview-cell-control-center :deep(.el-select__selected-item),
.preview-cell-control-center :deep(.el-select__placeholder) {
  margin-inline: auto;
}

.preview-cell-control-right :deep(.el-select__selected-item),
.preview-cell-control-right :deep(.el-select__placeholder) {
  margin-left: auto;
}

.preview-choice-group :deep(.el-radio),
.preview-choice-group :deep(.el-checkbox) {
  width: 100%;
  height: 24px;
  margin-right: 0;
  justify-content: flex-start;
}

.preview-choice-group :deep(.el-radio__label),
.preview-choice-group :deep(.el-checkbox__label) {
  min-width: 0;
  padding-left: 6px;
}

.preview-select-control :deep(.el-select__wrapper) {
  width: 100%;
  gap: 2px;
  justify-content: flex-start;
}

.preview-select-root :deep(.el-select) {
  display: block;
  width: 100%;
}

.preview-select-control :deep(.el-select__selection) {
  flex: 1;
  min-width: 0;
  max-width: 100%;
}

.preview-select-control :deep(.el-select__selected-item),
.preview-select-control :deep(.el-select__placeholder) {
  max-width: 100%;
}

.preview-select-control :deep(.el-select__placeholder) {
  position: static;
  width: auto;
  transform: none;
}

.preview-upload-control :deep(.el-upload) {
  display: flex;
  justify-content: center;
}

.preview-upload-control :deep(.el-upload-list) {
  margin: 0;
}

.preview-upload-control :deep(.el-upload-list:empty) {
  display: none;
}
</style>
