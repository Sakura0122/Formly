<script setup lang="ts">
import { computed, ref, watch } from 'vue'

import { EDITOR_TABLE_MIN_ROW_HEIGHT } from '@/constants/editor'
import type { EditorCanvasCell, EditorCanvasTable, EditorFieldInstance, EditorFormPreviewMode } from '@/types/editor'
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
const activeCellId = ref('')

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
      return 24
    case 'radio':
    case 'checkbox':
      return estimateChoiceFieldHeight(field)
    case 'upload':
      return 30
    case 'switch':
      return 24
    default:
      return 32
  }
}

const isCompactSingleField = (fieldList: EditorFieldInstance[]) => {
  if (fieldList.length !== 1) {
    return false
  }

  const field = fieldList[0]!

  return ['text', 'textbox', 'number', 'date'].includes(field.type)
}

const estimateCellHeight = (fieldList: EditorFieldInstance[]) => {
  if (!fieldList.length) {
    return EDITOR_TABLE_MIN_ROW_HEIGHT
  }

  if (isCompactSingleField(fieldList)) {
    return EDITOR_TABLE_MIN_ROW_HEIGHT
  }

  const contentHeight = fieldList.reduce((total, field) => {
    return total + estimateFieldHeight(field)
  }, 0)

  return Math.max(EDITOR_TABLE_MIN_ROW_HEIGHT, contentHeight + Math.max(0, fieldList.length - 1) * 4 + 8)
}

const visibleCells = computed(() => {
  return getVisibleCells(props.table)
})

const isEditableCell = (cell: EditorCanvasCell) => {
  return cell.fields.some((field) => !['text', 'image'].includes(field.type))
}

watch(
  visibleCells,
  (cellList) => {
    if (props.mode !== 'interactive') {
      activeCellId.value = ''
      return
    }

    const hasActiveCell = cellList.some((cell) => cell.id === activeCellId.value && isEditableCell(cell))

    if (hasActiveCell) {
      return
    }

    activeCellId.value = cellList.find((cell) => isEditableCell(cell))?.id ?? ''
  },
  {
    immediate: true,
  },
)

const effectiveRowHeights = computed(() => {
  const baseHeights = [...(props.table?.rowHeights ?? [])]

  visibleCells.value.forEach((cell) => {
    const estimatedHeight = estimateCellHeight(cell.fields)
    const spanIndexes = Array.from({ length: cell.rowSpan }, (_, index) => cell.row - 1 + index)
    const currentSpanHeight = spanIndexes.reduce((total, index) => {
      return total + (baseHeights[index] ?? EDITOR_TABLE_MIN_ROW_HEIGHT)
    }, 0)

    if (estimatedHeight <= currentSpanHeight) {
      return
    }

    const lastIndex = spanIndexes.at(-1)

    if (lastIndex === undefined) {
      return
    }

    baseHeights[lastIndex] =
      (baseHeights[lastIndex] ?? EDITOR_TABLE_MIN_ROW_HEIGHT) + estimatedHeight - currentSpanHeight
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

const handleCellClick = (cell: EditorCanvasCell) => {
  if (props.mode !== 'interactive' || !isEditableCell(cell)) {
    return
  }

  activeCellId.value = cell.id
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
        class="relative min-w-0 border-r border-b border-slate-800 transition-colors"
        :class="[
          mode === 'readonly' ? 'pointer-events-none bg-white' : '',
          mode === 'interactive' && isEditableCell(cell) ? 'cursor-text bg-emerald-50/45 hover:bg-emerald-50/65' : '',
          mode === 'interactive' && !isEditableCell(cell) ? 'bg-white' : '',
          activeCellId === cell.id ? 'z-10' : '',
        ]"
        :style="{
          gridColumn: `${cell.col} / span ${cell.colSpan}`,
          gridRow: `${cell.row} / span ${cell.rowSpan}`,
        }"
        @click="handleCellClick(cell)"
      >
        <div
          v-if="mode === 'interactive' && activeCellId === cell.id && isEditableCell(cell)"
          class="pointer-events-none absolute -inset-px border-2 border-emerald-500 shadow-[0_0_0_1px_rgba(16,185,129,0.12)]"
        />

        <div class="flex h-full min-w-0 flex-col justify-center gap-0.5 px-1.5 py-0">
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
