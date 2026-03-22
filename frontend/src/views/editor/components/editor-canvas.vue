<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import {
  EDITOR_TABLE_HEADER_HEIGHT,
  EDITOR_TABLE_MIN_COLUMN_WIDTH,
  EDITOR_TABLE_MIN_ROW_HEIGHT,
  EDITOR_TABLE_ROW_HEADER_WIDTH,
} from '@/constants/editor'
import type {
  EditorCanvasContextMenuPayload,
  EditorCanvasDropPayload,
  EditorCanvasSelectionPayload,
  EditorCanvasTable,
  EditorComponentType,
  EditorResizeColumnPayload,
  EditorResizeRowPayload,
  EditorSelectFieldPayload,
} from '@/types/editor'
import EditorFieldPreview from '@/views/editor/components/editor-field-preview.vue'
import {
  buildSelectionPayload,
  getCellRange,
  getVisibleCells,
} from '@/views/editor/utils/table'

const props = defineProps<{
  table: EditorCanvasTable | null
  activeCellId: string
  activeFieldId: string
  selectedCellIds: string[]
  selectionAnchorCellId: string
}>()

const emit = defineEmits<{
  (e: 'change-selection', payload: EditorCanvasSelectionPayload): void
  (e: 'select-field', payload: EditorSelectFieldPayload): void
  (e: 'place-item', payload: EditorCanvasDropPayload): void
  (e: 'context-menu', payload: EditorCanvasContextMenuPayload): void
  (e: 'resize-column', payload: EditorResizeColumnPayload): void
  (e: 'resize-row', payload: EditorResizeRowPayload): void
}>()

const selectionDraggingAnchorId = ref('')
const canvasViewportRef = ref<HTMLElement | null>(null)
const canvasViewportWidth = ref(0)
const canvasResizeObserver = ref<ResizeObserver | null>(null)
const resizeState = ref<
  | {
      type: 'column' | 'row'
      index: number
      startPointer: number
      startSize: number
    }
  | null
>(null)

const toColumnLabel = (index: number) => {
  let currentIndex = index
  let result = ''

  while (currentIndex > 0) {
    const remainder = (currentIndex - 1) % 26
    result = `${String.fromCharCode(65 + remainder)}${result}`
    currentIndex = Math.floor((currentIndex - 1) / 26)
  }

  return result
}

const columnHeaders = computed(() => {
  return Array.from({ length: props.table?.columns ?? 0 }, (_, index) => {
    return {
      index,
      columnNumber: index + 1,
      label: toColumnLabel(index + 1),
      width: 0,
    }
  })
})

const estimateFieldHeight = (type: EditorComponentType) => {
  switch (type) {
    case 'text':
      return 34
    case 'image':
      return 74
    case 'textarea':
      return 42
    case 'radio':
    case 'checkbox':
      return 28
    case 'upload':
      return 30
    case 'switch':
      return 24
    default:
      return 32
  }
}

const estimateCellHeight = (typeList: EditorComponentType[]) => {
  if (!typeList.length) {
    return EDITOR_TABLE_MIN_ROW_HEIGHT
  }

  const contentHeight = typeList.reduce((total, type) => {
    return total + estimateFieldHeight(type)
  }, 0)

  return Math.max(
    EDITOR_TABLE_MIN_ROW_HEIGHT,
    contentHeight + Math.max(0, typeList.length - 1) * 4 + 10,
  )
}

const effectiveRowHeights = computed(() => {
  const baseHeights = [...(props.table?.rowHeights ?? [])]

  getVisibleCells(props.table).forEach((cell) => {
    const estimatedHeight = estimateCellHeight(cell.fields.map((field) => field.type))
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

    baseHeights[lastIndex] = (baseHeights[lastIndex] ?? EDITOR_TABLE_MIN_ROW_HEIGHT) + estimatedHeight - currentSpanHeight
  })

  return baseHeights
})

const rowHeaders = computed(() => {
  return Array.from({ length: props.table?.rows ?? 0 }, (_, index) => {
    return {
      index,
      rowNumber: index + 1,
      height: effectiveRowHeights.value[index] ?? EDITOR_TABLE_MIN_ROW_HEIGHT,
    }
  })
})

const visibleCells = computed(() => {
  return getVisibleCells(props.table)
})

const displayColumnWidths = computed(() => {
  if (!props.table) {
    return []
  }

  const rawWidths = props.table.columnWidths
  const totalWidth = rawWidths.reduce((total, width) => total + width, 0)
  const availableWidth = Math.max(
    canvasViewportWidth.value - EDITOR_TABLE_ROW_HEADER_WIDTH - 32,
    0,
  )

  if (!totalWidth || !availableWidth || totalWidth <= availableWidth) {
    return rawWidths
  }

  const scale = availableWidth / totalWidth

  return rawWidths.map((width) => Math.max(1, Math.floor(width * scale)))
})

const displayTableWidth = computed(() => {
  return EDITOR_TABLE_ROW_HEADER_WIDTH + displayColumnWidths.value.reduce((total, width) => total + width, 0)
})

const displayColumnHeaders = computed(() => {
  return columnHeaders.value.map((column, index) => {
    return {
      ...column,
      width: displayColumnWidths.value[index] ?? props.table?.columnWidths[index] ?? 0,
    }
  })
})

const gridTemplateColumns = computed(() => {
  return `${EDITOR_TABLE_ROW_HEADER_WIDTH}px ${displayColumnHeaders.value
    .map((column) => `${column.width}px`)
    .join(' ')}`
})

const gridTemplateRows = computed(() => {
  return `${EDITOR_TABLE_HEADER_HEIGHT}px ${rowHeaders.value
    .map((row) => `${row.height}px`)
    .join(' ')}`
})

const createSingleSelection = (cellId: string): EditorCanvasSelectionPayload => {
  return {
    activeCellId: cellId,
    selectedCellIds: [cellId],
    selectionAnchorCellId: cellId,
  }
}

const selectedCellIdSet = computed(() => {
  return new Set(props.selectedCellIds)
})

const multiSelectionBounds = computed(() => {
  if (props.selectedCellIds.length < 2) {
    return null
  }

  const selectedCells = visibleCells.value.filter((cell) => selectedCellIdSet.value.has(cell.id))

  if (!selectedCells.length) {
    return null
  }

  return selectedCells.reduce(
    (bounds, cell) => {
      const range = getCellRange(cell)

      return {
        rowStart: Math.min(bounds.rowStart, range.rowStart),
        rowEnd: Math.max(bounds.rowEnd, range.rowEnd),
        colStart: Math.min(bounds.colStart, range.colStart),
        colEnd: Math.max(bounds.colEnd, range.colEnd),
      }
    },
    {
      rowStart: Number.POSITIVE_INFINITY,
      rowEnd: Number.NEGATIVE_INFINITY,
      colStart: Number.POSITIVE_INFINITY,
      colEnd: Number.NEGATIVE_INFINITY,
    },
  )
})

const emitRangeSelection = (anchorCellId: string, targetCellId: string) => {
  emit('change-selection', buildSelectionPayload(props.table, anchorCellId, targetCellId))
}

const handleCellMouseDown = (event: MouseEvent, cellId: string) => {
  if (event.button !== 0) {
    return
  }

  event.preventDefault()

  const anchorCellId = event.shiftKey && props.selectionAnchorCellId
    ? props.selectionAnchorCellId
    : cellId

  selectionDraggingAnchorId.value = anchorCellId

  if (event.shiftKey && props.selectionAnchorCellId) {
    emitRangeSelection(props.selectionAnchorCellId, cellId)
    return
  }

  emit('change-selection', createSingleSelection(cellId))
}

const handleCellMouseEnter = (cellId: string) => {
  if (!selectionDraggingAnchorId.value) {
    return
  }

  emitRangeSelection(selectionDraggingAnchorId.value, cellId)
}

const handleFieldClick = (cellId: string, fieldId: string) => {
  emit('change-selection', createSingleSelection(cellId))
  emit('select-field', {
    cellId,
    fieldId,
  })
}

const handleDrop = (event: DragEvent, cellId: string) => {
  const rawPayload = event.dataTransfer?.getData('application/json')

  if (!rawPayload) {
    return
  }

  const payload = JSON.parse(rawPayload) as { type?: EditorComponentType }

  if (!payload.type) {
    return
  }

  emit('place-item', {
    cellId,
    type: payload.type,
  })
}

const handleContextMenu = (event: MouseEvent, cellId = '') => {
  emit('context-menu', {
    x: event.clientX,
    y: event.clientY,
    cellId,
  })
}

const startColumnResize = (event: MouseEvent, index: number) => {
  resizeState.value = {
    type: 'column',
    index,
    startPointer: event.clientX,
    startSize: props.table?.columnWidths[index] ?? EDITOR_TABLE_MIN_COLUMN_WIDTH,
  }
}

const startRowResize = (event: MouseEvent, index: number) => {
  resizeState.value = {
    type: 'row',
    index,
    startPointer: event.clientY,
    startSize: props.table?.rowHeights[index] ?? EDITOR_TABLE_MIN_ROW_HEIGHT,
  }
}

const handleWindowMouseMove = (event: MouseEvent) => {
  if (!resizeState.value) {
    return
  }

  if (resizeState.value.type === 'column') {
    emit('resize-column', {
      index: resizeState.value.index,
      width: Math.max(
        EDITOR_TABLE_MIN_COLUMN_WIDTH,
        resizeState.value.startSize + event.clientX - resizeState.value.startPointer,
      ),
    })
    return
  }

  emit('resize-row', {
    index: resizeState.value.index,
    height: Math.max(
      EDITOR_TABLE_MIN_ROW_HEIGHT,
      resizeState.value.startSize + event.clientY - resizeState.value.startPointer,
    ),
  })
}

const clearDraggingState = () => {
  selectionDraggingAnchorId.value = ''
  resizeState.value = null
}

const syncCanvasViewportWidth = () => {
  canvasViewportWidth.value = canvasViewportRef.value?.clientWidth ?? 0
}

onMounted(() => {
  syncCanvasViewportWidth()
  canvasResizeObserver.value = new ResizeObserver(() => {
    syncCanvasViewportWidth()
  })
  canvasViewportRef.value && canvasResizeObserver.value.observe(canvasViewportRef.value)
  window.addEventListener('mousemove', handleWindowMouseMove)
  window.addEventListener('mouseup', clearDraggingState)
})

onBeforeUnmount(() => {
  canvasResizeObserver.value?.disconnect()
  window.removeEventListener('mousemove', handleWindowMouseMove)
  window.removeEventListener('mouseup', clearDraggingState)
})
</script>

<template>
  <section class="flex h-full min-h-0 border border-slate-200 bg-white">
    <div
      class="h-full w-full min-h-0"
      @contextmenu.prevent="handleContextMenu($event)"
    >
      <template v-if="table">
        <el-scrollbar class="h-full w-full">
          <div
            ref="canvasViewportRef"
            class="min-h-full p-4 text-center"
          >
            <div class="inline-block pr-4 text-left">
              <div
                class="relative grid shrink-0 border-l border-t border-slate-300 bg-white"
                :style="{
                  gridTemplateColumns,
                  gridTemplateRows,
                  width: `${displayTableWidth}px`,
                }"
              >
                <div
                  class="border-r border-b border-slate-300 bg-[#f2f6f7]"
                  :style="{
                    gridColumn: '1 / 2',
                    gridRow: '1 / 2',
                  }"
                />

                <div
                  v-for="column in displayColumnHeaders"
                  :key="column.columnNumber"
                  class="relative flex items-center justify-center border-r border-b border-slate-300 bg-[#f2f6f7] text-[10px] font-medium text-[#6f8686]"
                  :style="{
                    gridColumn: `${column.columnNumber + 1} / ${column.columnNumber + 2}`,
                    gridRow: '1 / 2',
                  }"
                >
                  {{ column.label }}
                  <button
                    v-if="column.index < columnHeaders.length - 1"
                    class="absolute inset-y-0 -right-0.5 z-10 w-1 cursor-col-resize"
                    type="button"
                    @mousedown.stop.prevent="startColumnResize($event, column.index)"
                  />
                </div>

                <div
                  v-for="row in rowHeaders"
                  :key="row.rowNumber"
                  class="relative flex items-center justify-center border-r border-b border-slate-300 bg-[#f2f6f7] text-[10px] font-medium text-[#6f8686]"
                  :class="selectedCellIdSet.size ? 'select-none' : ''"
                  :style="{
                    gridColumn: '1 / 2',
                    gridRow: `${row.rowNumber + 1} / ${row.rowNumber + 2}`,
                  }"
                >
                  {{ row.rowNumber }}
                  <button
                    class="absolute inset-x-0 -bottom-0.5 z-10 h-1 cursor-row-resize"
                    type="button"
                    @mousedown.stop.prevent="startRowResize($event, row.index)"
                  />
                </div>

                <div
                  v-if="multiSelectionBounds"
                  class="pointer-events-none z-2 border-2 border-emerald-500 bg-emerald-100/10"
                  :style="{
                    gridColumn: `${multiSelectionBounds.colStart + 1} / ${multiSelectionBounds.colEnd + 2}`,
                    gridRow: `${multiSelectionBounds.rowStart + 1} / ${multiSelectionBounds.rowEnd + 2}`,
                  }"
                />

                <div
                  v-for="cell in visibleCells"
                  :key="cell.id"
                  class="relative min-w-0 border-r border-b border-slate-300 bg-white px-0.5 py-0"
                  :class="[
                    selectedCellIdSet.has(cell.id) ? 'bg-emerald-50/35' : '',
                    activeCellId === cell.id ? 'z-1' : '',
                  ]"
                  :style="{
                    gridColumn: `${cell.col + 1} / span ${cell.colSpan}`,
                    gridRow: `${cell.row + 1} / span ${cell.rowSpan}`,
                  }"
                  @mousedown.stop.prevent="handleCellMouseDown($event, cell.id)"
                  @mouseover="handleCellMouseEnter(cell.id)"
                  @contextmenu.stop.prevent="handleContextMenu($event, cell.id)"
                  @dragover.prevent
                  @drop.prevent="handleDrop($event, cell.id)"
                >
                  <div
                    v-if="activeCellId === cell.id && props.selectedCellIds.length <= 1"
                    class="pointer-events-none absolute inset-0 border-2 border-sky-500"
                  />

                  <div
                    v-if="cell.fields.length"
                    class="flex h-full min-w-0 flex-col gap-0.5"
                    :class="cell.fields.length === 1 ? 'justify-center' : ''"
                  >
                    <div
                      v-for="field in cell.fields"
                      :key="field.uuid"
                      class="min-w-0 px-0 py-0 transition"
                      :class="
                        field.uuid === activeFieldId
                          ? 'bg-sky-50/45'
                          : ''
                      "
                      @click.stop="handleFieldClick(cell.id, field.uuid)"
                    >
                      <div class="pointer-events-none min-w-0">
                        <EditorFieldPreview :field="field" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </template>

      <template v-else>
        <div class="h-full p-4">
          <div class="h-full border border-slate-200 bg-white" />
        </div>
      </template>
    </div>
  </section>
</template>
