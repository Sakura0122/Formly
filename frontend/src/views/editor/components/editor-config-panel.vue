<script setup lang="ts">
import { computed } from 'vue'
import type { UploadFile } from 'element-plus'

import { EDITOR_PALETTE_ITEMS } from '@/constants/editor'
import type {
  EditorCanvasCell,
  EditorComponentType,
  EditorHorizontalAlign,
  EditorFieldOption,
  EditorFieldPatch,
  EditorFieldInstance,
} from '@/types/editor'

const props = defineProps<{
  /** 当前激活的单元格 */
  activeCell: EditorCanvasCell | null
  /** 当前激活的组件 */
  activeField: EditorFieldInstance | null
  /** 单元格组件列表 */
  cellFields: EditorFieldInstance[]
}>()

const emit = defineEmits<{
  /** 更新组件 */
  (e: 'update-field', patch: EditorFieldPatch): void
  /** 改变组件类型 */
  (e: 'change-field-type', type: EditorComponentType): void
  /** 选中组件 */
  (e: 'select-field', fieldId: string): void
  /** 删除组件 */
  (e: 'remove-field', fieldId: string): void
}>()

const typeOptions = computed(() => {
  return EDITOR_PALETTE_ITEMS.map((item) => {
    return {
      label: item.label,
      value: item.type,
    }
  })
})

const supportsPlaceholder = computed(() => {
  return (
    props.activeField?.type === 'textbox' ||
    props.activeField?.type === 'number' ||
    props.activeField?.type === 'select' ||
    props.activeField?.type === 'date'
  )
})

const supportsTextContent = computed(() => {
  return props.activeField?.type === 'text'
})

const supportsImage = computed(() => {
  return props.activeField?.type === 'image'
})

const supportsOptions = computed(() => {
  return (
    props.activeField?.type === 'radio' ||
    props.activeField?.type === 'checkbox' ||
    props.activeField?.type === 'select'
  )
})

const supportsSwitch = computed(() => {
  return props.activeField?.type === 'switch'
})

const supportsRequired = computed(() => {
  return props.activeField?.type !== 'text' && props.activeField?.type !== 'image'
})

const horizontalAlignOptions: Array<{ label: string; value: EditorHorizontalAlign }> = [
  {
    label: '居左',
    value: 'left',
  },
  {
    label: '居中',
    value: 'center',
  },
  {
    label: '居右',
    value: 'right',
  },
]

const readFileAsDataUrl = async (file: File) => {
  return await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = () => {
      if (typeof reader.result === 'string') {
        resolve(reader.result)
        return
      }

      reject(new Error('文件读取失败'))
    }

    reader.onerror = () => {
      reject(new Error('文件读取失败'))
    }

    reader.readAsDataURL(file)
  })
}

const handleImageFileChange = async (uploadFile: UploadFile) => {
  if (!uploadFile.raw) {
    return
  }

  const imageUrl = await readFileAsDataUrl(uploadFile.raw)

  emit('update-field', {
    imageUrl,
  })
}

const getComponentLabel = (type: EditorComponentType) => {
  return EDITOR_PALETTE_ITEMS.find((item) => item.type === type)?.label ?? type
}

const createOptionDraft = (index: number): EditorFieldOption => {
  return {
    label: `选项${index + 1}`,
    value: `option_${index + 1}`,
  }
}

const editableOptions = computed(() => {
  if (!props.activeField) {
    return []
  }

  return props.activeField.options.length ? props.activeField.options : [createOptionDraft(0)]
})

/**
 * 更新选项
 * @param index 选项索引
 * @param key 选项键
 * @param value 选项值
 */
const updateOption = (index: number, key: keyof EditorFieldOption, value: string) => {
  const nextOptions = editableOptions.value.map((option, optionIndex) => {
    if (optionIndex !== index) {
      return option
    }

    return {
      ...option,
      [key]: value,
    }
  })

  emit('update-field', {
    options: nextOptions,
  })
}

/**
 * 添加选项
 */
const addOption = () => {
  const nextOptions = [...editableOptions.value, createOptionDraft(editableOptions.value.length)]

  emit('update-field', {
    options: nextOptions,
  })
}

/**
 * 删除选项
 * @param index 选项索引
 */
const removeOption = (index: number) => {
  const nextOptions = editableOptions.value.filter((_, optionIndex) => optionIndex !== index)

  emit('update-field', {
    options: nextOptions.length ? nextOptions : [createOptionDraft(0)],
  })
}
</script>

<template>
  <aside class="flex h-full min-h-0 flex-col rounded-3xl border border-slate-200 bg-white">
    <div class="border-b border-slate-100 px-5 py-5">
      <h2 class="text-base font-semibold text-slate-900">配置面板</h2>
    </div>

    <el-scrollbar class="min-h-0 flex-1">
      <div class="space-y-4 p-5">
        <div class="rounded-2xl bg-slate-50 p-4">
          <div class="text-sm font-medium text-slate-900">当前单元格</div>
          <div class="mt-2 text-sm leading-6 text-slate-500">
            {{ activeCell ? `${activeCell.row} 行 ${activeCell.col} 列` : '尚未选中单元格' }}
          </div>
        </div>

        <div v-if="activeCell && cellFields.length" class="rounded-2xl border border-slate-200 p-4">
          <div class="mb-3 text-sm font-medium text-slate-900">单元格组件</div>
          <div class="flex flex-col gap-2">
            <button
              v-for="(field, index) in cellFields"
              :key="field.uuid"
              class="flex items-center justify-between rounded-xl border px-3 py-2 text-left transition"
              :class="
                field.uuid === activeField?.uuid
                  ? 'border-sky-400 bg-sky-50 text-sky-700'
                  : 'border-slate-200 bg-white text-slate-600'
              "
              type="button"
              @click="emit('select-field', field.uuid)"
            >
              <span class="text-sm">{{ index + 1 }}. {{ getComponentLabel(field.type) }}</span>
              <el-button circle plain size="small" @click.stop="emit('remove-field', field.uuid)">
                <span class="text-xs">×</span>
              </el-button>
            </button>
          </div>
        </div>

        <div v-if="activeField" class="rounded-2xl border border-slate-200 p-4">
          <div class="mb-4 text-sm font-medium text-slate-900">组件配置</div>
          <el-form label-position="top">
            <el-form-item label="组件类型">
              <el-select
                :model-value="activeField.type"
                placeholder="选择组件类型"
                style="width: 100%"
                @update:model-value="emit('change-field-type', $event as EditorComponentType)"
              >
                <el-option
                  v-for="option in typeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-if="supportsPlaceholder" label="占位提示">
              <el-input
                :model-value="activeField.placeholder"
                placeholder="请输入占位提示"
                @update:model-value="emit('update-field', { placeholder: String($event) })"
              />
            </el-form-item>

            <el-form-item label="水平对齐">
              <el-radio-group
                :model-value="activeField.horizontalAlign"
                size="small"
                @update:model-value="
                  emit('update-field', {
                    horizontalAlign: $event as EditorHorizontalAlign,
                  })
                "
              >
                <el-radio-button v-for="option in horizontalAlignOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="supportsTextContent" label="固定文字内容">
              <el-input
                :autosize="{ minRows: 2, maxRows: 4 }"
                :model-value="activeField.textContent"
                placeholder="请输入固定文字内容"
                type="textarea"
                @update:model-value="emit('update-field', { textContent: String($event) })"
              />
            </el-form-item>

            <el-form-item v-if="supportsImage" label="图片地址">
              <div class="flex w-full flex-col gap-3">
                <el-upload
                  :auto-upload="false"
                  :limit="1"
                  :on-change="handleImageFileChange"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-button plain>上传图片</el-button>
                </el-upload>

                <el-input
                  :model-value="activeField.imageUrl"
                  placeholder="请输入图片地址"
                  @update:model-value="emit('update-field', { imageUrl: String($event) })"
                />
              </div>
            </el-form-item>

            <el-form-item label="备注">
              <el-input
                :autosize="{ minRows: 2, maxRows: 4 }"
                :model-value="activeField.helpText"
                placeholder="请输入备注"
                type="textarea"
                @update:model-value="emit('update-field', { helpText: String($event) })"
              />
            </el-form-item>

            <el-form-item v-if="supportsRequired" label="是否必填">
              <el-switch
                :model-value="activeField.required"
                @update:model-value="emit('update-field', { required: Boolean($event) })"
              />
            </el-form-item>

            <el-form-item v-if="supportsOptions" label="选项列表">
              <div class="flex w-full flex-col gap-2">
                <div
                  v-for="(option, index) in editableOptions"
                  :key="`${index}-${option.value}`"
                  class="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)_36px] items-center gap-2"
                >
                  <el-input
                    :model-value="option.label"
                    placeholder="显示文案"
                    @update:model-value="updateOption(index, 'label', String($event))"
                  />
                  <el-input
                    :model-value="option.value"
                    placeholder="提交值"
                    @update:model-value="updateOption(index, 'value', String($event))"
                  />
                  <el-button circle plain @click="removeOption(index)">
                    <span class="text-xs">×</span>
                  </el-button>
                </div>

                <el-button plain @click="addOption">添加选项</el-button>
              </div>
            </el-form-item>

            <template v-if="supportsSwitch">
              <el-form-item label="开启文案">
                <el-input
                  :model-value="activeField.switchActiveText"
                  placeholder="启用"
                  @update:model-value="emit('update-field', { switchActiveText: String($event) })"
                />
              </el-form-item>

              <el-form-item label="关闭文案">
                <el-input
                  :model-value="activeField.switchInactiveText"
                  placeholder="停用"
                  @update:model-value="
                    emit('update-field', {
                      switchInactiveText: String($event),
                    })
                  "
                />
              </el-form-item>
            </template>
          </el-form>
        </div>

        <el-empty v-else description="选中组件后可在这里配置" />
      </div>
    </el-scrollbar>
  </aside>
</template>
