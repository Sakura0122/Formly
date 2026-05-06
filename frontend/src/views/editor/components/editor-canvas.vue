<script setup lang="ts">
import { useElementSize, useEventListener } from '@vueuse/core'
import { storeToRefs } from 'pinia'
import { computed, ref, useTemplateRef } from 'vue'

import formlyLogoMark from '@/assets/brand/formly-logo-mark.svg'
import {
  EDITOR_TABLE_HEADER_HEIGHT,
  EDITOR_TABLE_MIN_COLUMN_WIDTH,
  EDITOR_TABLE_MIN_ROW_HEIGHT,
  EDITOR_TABLE_ROW_HEADER_WIDTH,
} from '@/constants/editor'
import { useEditorStore } from '@/stores/editor'
import type {
  EditorCanvasContextMenuPayload,
  EditorCanvasSelectionPayload,
  EditorCanvasTable,
  EditorComponentType,
} from '@/types/editor'
import EditorFieldPreview from '@/views/editor/components/editor-field-preview.vue'
import { buildSelectionPayload, getCellRange, getVisibleCells } from '@/views/editor/utils/table'

const { table: readonlyTable = null, readonly = false } = defineProps<{
  table?: EditorCanvasTable | null
  readonly?: boolean
}>()

const emit = defineEmits<{
  /** 右键菜单 */
  (e: 'context-menu', payload: EditorCanvasContextMenuPayload): void
}>()

const editorStore = useEditorStore()
const {
  activeCellId: storeActiveCellId,
  activeFieldId: storeActiveFieldId,
  selectedCellIds: storeSelectedCellIds,
  selectionAnchorCellId: storeSelectionAnchorCellId,
  table: storeTable,
} = storeToRefs(editorStore)

const canvasTable = computed(() => {
  return readonly ? readonlyTable : storeTable.value
})

const activeCellId = computed(() => {
  return readonly ? '' : storeActiveCellId.value
})

const activeFieldId = computed(() => {
  return readonly ? '' : storeActiveFieldId.value
})

const selectedCellIds = computed(() => {
  return readonly ? [] : storeSelectedCellIds.value
})

const selectionAnchorCellId = computed(() => {
  return readonly ? '' : storeSelectionAnchorCellId.value
})

// 框选的锚点单元格id
const selectionDraggingAnchorId = ref('')
const canvasViewportRef = useTemplateRef<HTMLElement | null>('canvasViewportRef')
const resizeState = ref<{
  type: 'column' | 'row'
  index: number
  startPointer: number
  startSize: number
} | null>(null)

const { width: canvasViewportWidth } = useElementSize(canvasViewportRef)

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

// {"index": 0, "columnNumber": 1, "label": "A", "width": 0}
const columnHeaders = computed(() => {
  return Array.from({ length: canvasTable.value?.columns ?? 0 }, (_, index) => {
    return {
      index,
      columnNumber: index + 1,
      label: toColumnLabel(index + 1),
      width: 0,
    }
  })
})

// [{index: 0, rowNumber: 1, height: 25}]
const rowHeaders = computed(() => {
  return Array.from({ length: canvasTable.value?.rows ?? 0 }, (_, index) => {
    return {
      index,
      rowNumber: index + 1,
      height: canvasTable.value?.rowHeights[index] ?? EDITOR_TABLE_MIN_ROW_HEIGHT,
    }
  })
})

const visibleCells = computed(() => {
  return getVisibleCells(canvasTable.value)
})

// 自适应列宽计算 [120, 120, 120]
const displayColumnWidths = computed(() => {
  if (!canvasTable.value) {
    return []
  }

  // 原始列宽数组（用户设置/默认值）
  const rawWidths = canvasTable.value.columnWidths
  // 所有列宽之和
  const totalWidth = rawWidths.reduce((total, width) => total + width, 0)
  // 可用宽度（视口宽度 - 行号列 - padding）
  const availableWidth = Math.max(canvasViewportWidth.value - EDITOR_TABLE_ROW_HEADER_WIDTH - 32, 0)

  // 表格没超出视口 → 直接用原始宽度
  if (!totalWidth || !availableWidth || totalWidth <= availableWidth) {
    return rawWidths
  }

  // 表格超出视口 → 按比例缩放
  const scale = availableWidth / totalWidth

  return rawWidths.map((width) => Math.max(1, Math.floor(width * scale)))
})

// 表格实际渲染宽度 32 + 120 + 120 + 120 = 392
const displayTableWidth = computed(() => {
  // 行号列宽度 + 所有（经过自适应后的）列宽之和
  return EDITOR_TABLE_ROW_HEADER_WIDTH + displayColumnWidths.value.reduce((total, width) => total + width, 0)
})

// [{index: 0, columnNumber: 1, label: "A", width: 120}]
const displayColumnHeaders = computed(() => {
  return columnHeaders.value.map((column, index) => {
    return {
      ...column,
      width: displayColumnWidths.value[index] ?? canvasTable.value?.columnWidths[index] ?? 0,
    }
  })
})

// 32px 120px 120px 120px
const gridTemplateColumns = computed(() => {
  return `${EDITOR_TABLE_ROW_HEADER_WIDTH}px ${displayColumnHeaders.value
    .map((column) => `${column.width}px`)
    .join(' ')}`
})

// 行高使用 minmax(用户设定行高, auto)，内容超出时由 CSS Grid 自动撑开
const gridTemplateRows = computed(() => {
  return `${EDITOR_TABLE_HEADER_HEIGHT}px ${rowHeaders.value.map((row) => `minmax(${row.height}px, auto)`).join(' ')}`
})

const createSingleSelection = (cellId: string): EditorCanvasSelectionPayload => {
  return {
    activeCellId: cellId,
    selectedCellIds: [cellId],
    selectionAnchorCellId: cellId,
  }
}

const selectedCellIdSet = computed(() => {
  return new Set(selectedCellIds.value)
})

const selectedHeaderIndexes = computed(() => {
  const rows = new Set<number>()
  const columns = new Set<number>()

  if (!selectedCellIdSet.value.size) {
    return {
      rows,
      columns,
    }
  }

  visibleCells.value.forEach((cell) => {
    if (!selectedCellIdSet.value.has(cell.id)) {
      return
    }

    const range = getCellRange(cell)

    for (let row = range.rowStart; row <= range.rowEnd; row += 1) {
      rows.add(row)
    }

    for (let column = range.colStart; column <= range.colEnd; column += 1) {
      columns.add(column)
    }
  })

  return {
    rows,
    columns,
  }
})

const multiSelectionBounds = computed(() => {
  if (selectedCellIds.value.length < 2) {
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

/**
 * 触发单元格范围选择
 * @param anchorCellId 起始格
 * @param targetCellId 终点格
 */
const emitRangeSelection = (anchorCellId: string, targetCellId: string) => {
  editorStore.applySelection(buildSelectionPayload(canvasTable.value, anchorCellId, targetCellId))
}

const handleCellMouseDown = (event: MouseEvent, cellId: string) => {
  if (readonly) {
    return
  }

  // 0 代表左键，1 通常是中键，2 是右键
  if (event.button !== 0) {
    return
  }

  event.preventDefault()

  // 如果当前按着 Shift，并且组件里已经有旧的锚点就继续沿用旧锚点，否则就把当前点击的格子 cellId 当作新的锚点
  selectionDraggingAnchorId.value = event.shiftKey && selectionAnchorCellId.value ? selectionAnchorCellId.value : cellId

  if (event.shiftKey && selectionAnchorCellId.value) {
    emitRangeSelection(selectionAnchorCellId.value, cellId)
    return
  }

  editorStore.applySelection(createSingleSelection(cellId))
}

const handleCellMouseEnter = (cellId: string) => {
  if (readonly) {
    return
  }

  // 是否正在拖选
  // 如果锚点为空，说明用户只是普通移动鼠标，并没有按住鼠标在拖选
  if (!selectionDraggingAnchorId.value) {
    return
  }

  emitRangeSelection(selectionDraggingAnchorId.value, cellId)
}

const handleCanvasBlankMouseDown = (event: MouseEvent) => {
  if (readonly) {
    return
  }

  if (event.button !== 0 || event.target !== event.currentTarget) {
    return
  }

  editorStore.resetSelection()
}

/**
 * 处理组件点击
 * @param cellId 单元格id
 * @param fieldId 组件id
 */
const handleFieldClick = (cellId: string, fieldId: string) => {
  if (readonly) {
    return
  }

  // 把当前单元格设为选中状态
  editorStore.applySelection(createSingleSelection(cellId))
  // 选中组件
  editorStore.selectField({
    cellId,
    fieldId,
  })
}

const handleDrop = (event: DragEvent, cellId: string) => {
  if (readonly) {
    return
  }

  const rawPayload = event.dataTransfer?.getData('application/json')

  if (!rawPayload) {
    return
  }

  const payload = JSON.parse(rawPayload) as { type?: EditorComponentType }

  if (!payload.type) {
    return
  }

  editorStore.placeItem({
    cellId,
    type: payload.type,
  })
}

// 打开右键菜单
const handleContextMenu = (event: MouseEvent, cellId = '') => {
  if (readonly) {
    return
  }

  if (cellId && canvasTable.value && !selectedCellIds.value.includes(cellId)) {
    editorStore.collapseSelectionToCell(cellId)
  }

  emit('context-menu', {
    x: event.clientX,
    y: event.clientY,
    cellId,
  })
}

/**
 * 开始调整列宽
 * @param event 鼠标事件
 * @param index 列索引
 */
const startColumnResize = (event: MouseEvent, index: number) => {
  if (readonly) {
    return
  }

  resizeState.value = {
    type: 'column',
    index,
    startPointer: event.clientX,
    startSize: canvasTable.value?.columnWidths[index] ?? EDITOR_TABLE_MIN_COLUMN_WIDTH,
  }
}

/**
 * 开始调整行高
 * @param event 鼠标事件
 * @param index 行索引
 */
const startRowResize = (event: MouseEvent, index: number) => {
  if (readonly) {
    return
  }

  resizeState.value = {
    type: 'row',
    index,
    startPointer: event.clientY,
    startSize: canvasTable.value?.rowHeights[index] ?? EDITOR_TABLE_MIN_ROW_HEIGHT,
  }
}

const handleWindowMouseMove = (event: MouseEvent) => {
  if (!resizeState.value) {
    return
  }

  if (resizeState.value.type === 'column') {
    editorStore.resizeColumn({
      index: resizeState.value.index,
      width: Math.max(
        EDITOR_TABLE_MIN_COLUMN_WIDTH,
        resizeState.value.startSize + event.clientX - resizeState.value.startPointer,
      ),
    })
    return
  }

  editorStore.resizeRow({
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

useEventListener(window, 'mousemove', handleWindowMouseMove)
useEventListener(window, 'mouseup', clearDraggingState)
</script>

<template>
  <section class="h-full min-h-0 bg-white">
    <template v-if="canvasTable">
      <el-scrollbar class="h-full w-full">
        <div ref="canvasViewportRef" class="min-h-full p-4 text-center" @mousedown="handleCanvasBlankMouseDown">
          <div class="inline-block text-left">
            <div
              class="relative grid shrink-0 border-l border-t border-slate-300 bg-white"
              :style="{
                gridTemplateColumns,
                gridTemplateRows,
                width: `${displayTableWidth}px`,
              }"
              @contextmenu.stop.prevent="handleContextMenu($event)"
            >
              <!-- 左上角的空白格 -->
              <div
                class="border-r border-b border-slate-300 bg-[#f2f6f7]"
                :style="{
                  gridColumn: '1 / 2',
                  gridRow: '1 / 2',
                }"
              />

              <!-- 列头 -->
              <div
                v-for="column in displayColumnHeaders"
                :key="column.columnNumber"
                class="relative flex items-center justify-center border-r border-b border-slate-300 text-[10px] font-medium"
                :class="
                  selectedHeaderIndexes.columns.has(column.columnNumber)
                    ? 'border-b-emerald-500 bg-[#d3eee6] text-emerald-700'
                    : 'bg-[#f2f6f7] text-[#6f8686]'
                "
                :style="{
                  gridColumn: `${column.columnNumber + 1} / ${column.columnNumber + 2}`,
                  gridRow: '1 / 2',
                }"
              >
                {{ column.label }}
                <button
                  v-if="!readonly"
                  class="absolute inset-y-0 -right-0.5 z-10 w-1 cursor-col-resize"
                  @mousedown.stop.prevent="startColumnResize($event, column.index)"
                />
              </div>

              <!-- 行头 -->
              <div
                v-for="row in rowHeaders"
                :key="row.rowNumber"
                class="relative flex items-center justify-center border-r border-b border-slate-300 text-[10px] font-medium"
                :class="[
                  selectedHeaderIndexes.rows.has(row.rowNumber)
                    ? 'border-r-emerald-500 bg-[#d3eee6] text-emerald-700'
                    : 'bg-[#f2f6f7] text-[#6f8686]',
                  selectedCellIdSet.size ? 'select-none' : '',
                ]"
                :style="{
                  gridColumn: '1 / 2',
                  gridRow: `${row.rowNumber + 1} / ${row.rowNumber + 2}`,
                }"
              >
                {{ row.rowNumber }}
                <button
                  v-if="!readonly"
                  class="absolute inset-x-0 -bottom-0.5 z-10 h-1 cursor-row-resize"
                  @mousedown.stop.prevent="startRowResize($event, row.index)"
                />
              </div>

              <!-- 多选边框 -->
              <div
                v-if="multiSelectionBounds"
                class="pointer-events-none z-2 border-2 border-emerald-500"
                :style="{
                  gridColumn: `${multiSelectionBounds.colStart + 1} / ${multiSelectionBounds.colEnd + 2}`,
                  gridRow: `${multiSelectionBounds.rowStart + 1} / ${multiSelectionBounds.rowEnd + 2}`,
                  backgroundColor: 'color-mix(in srgb, var(--el-color-primary) 30%, transparent)',
                }"
              />

              <div
                v-for="cell in visibleCells"
                :key="cell.id"
                class="relative min-w-0 border-r border-b border-slate-300 bg-white px-0.5 py-0"
                :class="[
                  !readonly && selectedCellIdSet.has(cell.id) ? 'bg-emerald-50/35' : '',
                  !readonly && activeCellId === cell.id ? 'z-1' : '',
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
                  v-if="!readonly && activeCellId === cell.id && selectedCellIds.length <= 1"
                  class="pointer-events-none absolute inset-0 border-2 border-emerald-500"
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
                    :class="[
                      !readonly && field.uuid === activeFieldId ? 'bg-sky-50/45' : '',
                      cell.fields.length === 1 ? 'flex h-full items-center' : '',
                    ]"
                    @click.stop="handleFieldClick(cell.id, field.uuid)"
                  >
                    <div class="pointer-events-none min-w-0 w-full">
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
      <div class="h-full p-4" @contextmenu.prevent="handleContextMenu($event)">
        <div class="flex h-full items-center justify-center border border-slate-200 bg-white px-6">
          <div class="flex max-w-md flex-col items-center text-center">
            <img :src="formlyLogoMark" alt="Formly" class="h-24 w-24 object-contain" />
            <p class="mt-6 text-base font-medium text-slate-700">复制 Word / Excel 表格后可直接粘贴到此处</p>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
