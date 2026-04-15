<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import type { EditorCanvasTable, EditorFieldInstance, EditorFormPreviewMode } from '@/types/editor'
import { getVisibleCells } from '@/views/editor/utils/table'
import FormPreviewField from '@/components/form-preview/form-preview-field.vue'

const props = withDefaults(
  defineProps<{
    table: EditorCanvasTable | null
    mode?: EditorFormPreviewMode
  }>(),
  {
    mode: 'readonly',
  },
)

const fieldValueMap = ref<Record<string, unknown>>({})

const buildFieldInitialValue = (field: EditorFieldInstance) => {
  switch (field.type) {
    case 'checkbox':
      return []
    case 'switch':
      return false
    case 'upload':
      return []
    default:
      return ''
  }
}

watch(
  () => props.table,
  (table) => {
    const nextValues: Record<string, unknown> = {}

    getVisibleCells(table).forEach((cell) => {
      cell.fields.forEach((field) => {
        nextValues[field.uuid] = fieldValueMap.value[field.uuid] ?? buildFieldInitialValue(field)
      })
    })

    fieldValueMap.value = nextValues
  },
  {
    immediate: true,
  },
)

const estimateChoiceFieldHeight = (field: EditorFieldInstance) => {
  const optionCount = Math.max(field.options.length, 1)

  if (props.mode === 'interactive') {
    return optionCount * 24 + Math.max(optionCount - 1, 0) * 4 + 8
  }

  return optionCount * 16 + Math.max(optionCount - 1, 0) * 2 + 4
}

const estimateFieldHeight = (field: EditorFieldInstance) => {
  switch (field.type) {
    case 'text':
      return 24
    case 'image':
      return 74
    case 'textbox':
    case 'number':
    case 'date':
    case 'select':
      return props.mode === 'interactive' ? 36 : 24
    case 'radio':
    case 'checkbox':
      return estimateChoiceFieldHeight(field)
    case 'upload':
      return props.mode === 'interactive' ? 40 : 30
    case 'switch':
      return 24
    default:
      return 32
  }
}

const estimateCellHeight = (fieldList: EditorFieldInstance[]) => {
  if (!fieldList.length) {
    return 28
  }

  const contentHeight = fieldList.reduce((total, field) => {
    return total + estimateFieldHeight(field)
  }, 0)

  return Math.max(28, contentHeight + Math.max(0, fieldList.length - 1) * 6 + 10)
}

const visibleCells = computed(() => {
  return getVisibleCells(props.table)
})

const effectiveRowHeights = computed(() => {
  const baseHeights = [...(props.table?.rowHeights ?? [])]

  visibleCells.value.forEach((cell) => {
    const estimatedHeight = estimateCellHeight(cell.fields)
    const spanIndexes = Array.from({ length: cell.rowSpan }, (_, index) => cell.row - 1 + index)
    const currentSpanHeight = spanIndexes.reduce((total, index) => {
      return total + (baseHeights[index] ?? 28)
    }, 0)

    if (estimatedHeight <= currentSpanHeight) {
      return
    }

    const lastIndex = spanIndexes.at(-1)

    if (lastIndex === undefined) {
      return
    }

    baseHeights[lastIndex] = (baseHeights[lastIndex] ?? 28) + estimatedHeight - currentSpanHeight
  })

  return baseHeights
})

const gridTemplateColumns = computed(() => {
  return (props.table?.columnWidths ?? []).map((width) => `${width}px`).join(' ')
})

const gridTemplateRows = computed(() => {
  return effectiveRowHeights.value.map((height) => `minmax(${height}px, auto)`).join(' ')
})

const tableWidth = computed(() => {
  return (props.table?.columnWidths ?? []).reduce((total, width) => total + width, 0)
})

const updateFieldValue = (fieldId: string, value: unknown) => {
  fieldValueMap.value = {
    ...fieldValueMap.value,
    [fieldId]: value,
  }
}
</script>

<template>
  <div v-if="table" class="inline-block text-left align-top">
    <div
      class="relative grid border-l border-t border-slate-800 bg-white"
      :style="{
        gridTemplateColumns,
        gridTemplateRows,
        width: `${tableWidth}px`,
      }"
    >
      <div
        v-for="cell in visibleCells"
        :key="cell.id"
        class="min-w-0 border-r border-b border-slate-800 bg-white"
        :class="mode === 'readonly' ? 'pointer-events-none' : ''"
        :style="{
          gridColumn: `${cell.col} / span ${cell.colSpan}`,
          gridRow: `${cell.row} / span ${cell.rowSpan}`,
        }"
      >
        <div class="flex h-full min-w-0 flex-col justify-center gap-1 px-1.5 py-1">
          <div v-for="field in cell.fields" :key="field.uuid" class="min-w-0">
            <FormPreviewField
              :field="field"
              :mode="mode"
              :model-value="fieldValueMap[field.uuid]"
              @update:model-value="updateFieldValue(field.uuid, $event)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
