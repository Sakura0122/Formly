<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  EDITOR_DEFAULT_IMAGE_URL,
  EDITOR_DEFAULT_OPTIONS,
  EDITOR_HEADER_ACTION_OPTIONS,
  EDITOR_TABLE_DEFAULT_COLUMN_WIDTH,
  EDITOR_TABLE_DEFAULT_ROW_HEIGHT,
  EDITOR_TABLE_MIN_COLUMN_WIDTH,
} from '@/constants/editor'
import type {
  EditorCanvasContextMenuPayload,
  EditorCanvasDropPayload,
  EditorCanvasSelectionPayload,
  EditorCanvasTable,
  EditorComponentType,
  EditorContextMenuCommand,
  EditorContextMenuItem,
  EditorCreateTableForm,
  EditorFieldInstance,
  EditorFieldOption,
  EditorFieldPatch,
  EditorHeaderActionKey,
  EditorPaletteItem,
  EditorRemoveFieldPayload,
  EditorResizeColumnPayload,
  EditorResizeRowPayload,
} from '@/types/editor'
import EditorCanvas from '@/views/editor/components/editor-canvas.vue'
import EditorComponentPalette from '@/views/editor/components/editor-component-palette.vue'
import EditorConfigPanel from '@/views/editor/components/editor-config-panel.vue'
import EditorContextMenu from '@/views/editor/components/editor-context-menu.vue'
import EditorCreateTableDialog from '@/views/editor/components/editor-create-table-dialog.vue'
import EditorHeader from '@/views/editor/components/editor-header.vue'
import {
  createEditorTable,
  createUUID,
  deleteColumn,
  deleteRow,
  getCellById,
  insertColumnRight,
  insertRowBelow,
  mergeSelectedCells,
  splitMergedCell,
  validateMergeSelection,
} from '@/views/editor/utils/table'

defineOptions({
  name: 'EditorPage',
})

const router = useRouter()

const dirty = ref(false)
const saveLoading = ref(false)
const publishLoading = ref(false)
const table = ref<EditorCanvasTable | null>(null)
/** 当前激活的单元格id */
const activeCellId = ref('')
/** 当前激活的组件id */
const activeFieldId = ref('')
/** 整块选区的id列表 - 浅绿色高亮边框 */
const selectedCellIds = ref<string[]>([])
/** 范围选择的起点id - Shift 选区和鼠标拖选用 */
const selectionAnchorCellId = ref('')
const tableDialogRef = ref<InstanceType<typeof EditorCreateTableDialog>>()
const contextMenuRef = ref<InstanceType<typeof EditorContextMenu>>()
const pendingTableMode = ref<'create' | 'rebuild'>('create')

const cloneOptions = (options: EditorFieldOption[]) => {
  return options.map((option) => ({
    ...option,
  }))
}

/**
 * 创建组件默认值
 * @param type 组件类型
 */
const createFieldDefaults = (type: EditorComponentType) => {
  const options = ['radio', 'checkbox', 'select'].includes(type)
    ? cloneOptions(EDITOR_DEFAULT_OPTIONS)
    : []

  const placeholderMap: Record<EditorComponentType, string> = {
    text: '',
    image: '',
    textbox: '',
    number: '',
    radio: '',
    checkbox: '',
    select: '请选择',
    date: 'YYYY-MM-DD',
    switch: '',
    upload: '',
  }

  return {
    placeholder: placeholderMap[type],
    horizontalAlign: 'center' as const,
    textContent: type === 'text' ? '固定文字内容' : '',
    imageUrl: type === 'image' ? EDITOR_DEFAULT_IMAGE_URL : '',
    options,
    switchActiveText: '开启',
    switchInactiveText: '关闭',
  }
}

/**
 * 创建组件实例
 * @param type 组件类型
 * @returns 组件实例
 */
const createFieldInstance = (type: EditorComponentType): EditorFieldInstance => {
  const defaults = createFieldDefaults(type)

  return {
    uuid: createUUID(),
    type,
    placeholder: defaults.placeholder,
    required: false,
    helpText: '',
    horizontalAlign: defaults.horizontalAlign,
    textContent: defaults.textContent,
    imageUrl: defaults.imageUrl,
    options: defaults.options,
    switchActiveText: defaults.switchActiveText,
    switchInactiveText: defaults.switchInactiveText,
  }
}

/**
 * 当前激活的单元格
 */
const activeCell = computed(() => {
  return getCellById(table.value, activeCellId.value)
})

/**
 * 当前激活单元格的组件列表
 */
const cellFields = computed(() => {
  return activeCell.value?.fields ?? []
})

/**
 * 当前激活的组件
 */
const activeField = computed(() => {
  return cellFields.value.find((field) => field.uuid === activeFieldId.value) ?? null
})

/**
 * 合并单元格验证
 */
const mergeValidation = computed(() => {
  return validateMergeSelection(table.value, selectedCellIds.value)
})

/**
 * 替换单元格
 * @param cellId 单元格id
 * @param updater 单元格更新器
 */
const replaceCell = (
  cellId: string,
  updater: (cell: NonNullable<typeof activeCell.value>) => NonNullable<typeof activeCell.value>,
) => {
  if (!table.value) {
    return
  }

  table.value = {
    ...table.value,
    cells: table.value.cells.map((cell) => {
      if (cell.id !== cellId) {
        return cell
      }

      return updater(cell)
    }),
  }
}

/**
 * 同步激活组件
 * @param cellId 单元格id
 */
const syncActiveFieldByCell = (cellId: string) => {
  const nextCell = getCellById(table.value, cellId)

  if (!nextCell) {
    activeFieldId.value = ''
    return
  }

  activeFieldId.value = nextCell.fields.at(-1)?.uuid ?? ''
}

/**
 * 应用单元格选择
 * @param payload 单元格选择
 */
const applySelection = (payload: EditorCanvasSelectionPayload) => {
  activeCellId.value = payload.activeCellId
  selectedCellIds.value = payload.selectedCellIds
  selectionAnchorCellId.value = payload.selectionAnchorCellId
  syncActiveFieldByCell(payload.activeCellId)
}

const collapseSelectionToCell = (cellId: string) => {
  applySelection({
    activeCellId: cellId,
    selectedCellIds: [cellId],
    selectionAnchorCellId: cellId,
  })
}

/**
 * 重置选区
 */
const resetEditorSelection = () => {
  activeCellId.value = ''
  activeFieldId.value = ''
  selectedCellIds.value = []
  selectionAnchorCellId.value = ''
}

const openTableDialog = (mode: 'create' | 'rebuild') => {
  pendingTableMode.value = mode
  tableDialogRef.value?.open(mode)
}

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }

  ElMessage.info('当前没有可返回的历史页面')
}

const handlePreview = () => {
  ElMessage.info('预览能力将在后续阶段接入')
}

const handleSave = async () => {
  saveLoading.value = true
  dirty.value = false
  await Promise.resolve()
  saveLoading.value = false
  ElMessage.success('保存事件已输出，等待后续接入真实逻辑')
}

const handlePublish = async () => {
  publishLoading.value = true
  dirty.value = false
  await Promise.resolve()
  publishLoading.value = false
  ElMessage.success('发布事件已输出，等待后续接入真实逻辑')
}

const handleMoreAction = (action: EditorHeaderActionKey) => {
  const currentAction = EDITOR_HEADER_ACTION_OPTIONS.find((item) => item.key === action)
  ElMessage.info(`更多操作占位：${currentAction?.label ?? action}`)
}

/**
 * 处理创建表格
 * @param payload 表格创建表单
 */
const handleCreateTable = (payload: EditorCreateTableForm) => {
  table.value = createEditorTable(
    payload.rows,
    payload.columns,
    EDITOR_TABLE_DEFAULT_COLUMN_WIDTH,
    EDITOR_TABLE_DEFAULT_ROW_HEIGHT,
  )
  resetEditorSelection()

  dirty.value = pendingTableMode.value === 'rebuild' ? true : dirty.value
}

/**
 * 添加组件到单元格
 * @param cellId 单元格id
 * @param type 组件类型
 */
const appendFieldToCell = (cellId: string, type: EditorComponentType) => {
  if (!table.value) {
    ElMessage.warning('请先在画布区域右键新建表格')
    return
  }

  const nextField = createFieldInstance(type)

  replaceCell(cellId, (cell) => ({
    ...cell,
    fields: [...cell.fields, nextField],
  }))

  collapseSelectionToCell(cellId)
  activeFieldId.value = nextField.uuid
  dirty.value = true
}

/**
 * 处理点击的组件
 * @param item 组件
 */
const handleSelectItem = (item: EditorPaletteItem) => {
  if (!activeCellId.value) {
    ElMessage.warning('请先选择单元格')
    return
  }

  appendFieldToCell(activeCellId.value, item.type)
}

/**
 * 处理放置组件
 * @param payload 放置组件参数
 */
const handlePlaceItem = (payload: EditorCanvasDropPayload) => {
  appendFieldToCell(payload.cellId, payload.type)
}

/**
 * 处理选中的单元格
 * @param payload 选中的单元格参数
 */
const handleChangeSelection = (payload: EditorCanvasSelectionPayload) => {
  applySelection(payload)
}

/**
 * 处理清空选区
 */
const handleClearSelection = () => {
  resetEditorSelection()
}

/**
 * 处理点击组件
 * @param cellId 单元格id
 * @param fieldId 组件id
 */
const handleSelectField = ({ cellId, fieldId }: { cellId: string; fieldId: string }) => {
  collapseSelectionToCell(cellId)
  activeFieldId.value = fieldId
}

/**
 * 处理移除组件
 * @param cellId 单元格id
 * @param fieldId 组件id
 */
const handleRemoveField = ({ cellId, fieldId }: EditorRemoveFieldPayload) => {
  const targetCell = getCellById(table.value, cellId)

  if (!targetCell) {
    return
  }

  const nextFields = targetCell.fields.filter((field) => field.uuid !== fieldId)

  replaceCell(cellId, (cell) => ({
    ...cell,
    fields: nextFields,
  }))

  collapseSelectionToCell(cellId)
  activeFieldId.value = nextFields.at(-1)?.uuid ?? ''
  dirty.value = true
}

/**
 * 更新组件
 * @param patch 组件更新
 */
const handleUpdateField = (patch: EditorFieldPatch) => {
  if (!activeCell.value || !activeField.value) {
    return
  }

  replaceCell(activeCell.value.id, (cell) => ({
    ...cell,
    fields: cell.fields.map((field) => {
      if (field.uuid !== activeField.value?.uuid) {
        return field
      }

      return {
        ...field,
        ...patch,
      }
    }),
  }))

  dirty.value = true
}

/**
 * 改变组件类型
 * @param type 组件类型
 */
const handleChangeFieldType = (type: EditorComponentType) => {
  if (!activeField.value) {
    return
  }

  const defaults = createFieldDefaults(type)
  const currentField = activeField.value

  handleUpdateField({
    type,
    placeholder: defaults.placeholder,
    horizontalAlign: currentField.horizontalAlign || defaults.horizontalAlign,
    textContent:
      type === 'text' ? currentField.textContent || defaults.textContent : defaults.textContent,
    imageUrl: type === 'image' ? currentField.imageUrl || defaults.imageUrl : defaults.imageUrl,
    options:
      type === 'radio' || type === 'checkbox' || type === 'select'
        ? currentField.options.length
          ? cloneOptions(currentField.options)
          : defaults.options
        : [],
    switchActiveText:
      type === 'switch'
        ? currentField.switchActiveText || defaults.switchActiveText
        : defaults.switchActiveText,
    switchInactiveText:
      type === 'switch'
        ? currentField.switchInactiveText || defaults.switchInactiveText
        : defaults.switchInactiveText,
  })
}

const handleRemoveActiveField = (fieldId: string) => {
  if (!activeCell.value) {
    return
  }

  handleRemoveField({
    cellId: activeCell.value.id,
    fieldId,
  })
}

const handleResizeColumn = ({ index, width }: EditorResizeColumnPayload) => {
  if (!table.value) {
    return
  }

  const nextColumnWidths = [...table.value.columnWidths]
  const currentWidth = nextColumnWidths[index]
  const nextWidth = nextColumnWidths[index + 1]

  if (currentWidth === undefined) {
    return
  }

  if (nextWidth === undefined) {
    nextColumnWidths[index] = width
  } else {
    const delta = width - currentWidth
    const shrinkableDelta = Math.min(delta, nextWidth - EDITOR_TABLE_MIN_COLUMN_WIDTH)
    const growableDelta = Math.max(delta, -(currentWidth - EDITOR_TABLE_MIN_COLUMN_WIDTH))
    const appliedDelta = delta >= 0 ? Math.max(0, shrinkableDelta) : Math.min(0, growableDelta)

    nextColumnWidths[index] = currentWidth + appliedDelta
    nextColumnWidths[index + 1] = nextWidth - appliedDelta
  }

  table.value = {
    ...table.value,
    columnWidths: nextColumnWidths,
  }
  dirty.value = true
}

const handleResizeRow = ({ index, height }: EditorResizeRowPayload) => {
  if (!table.value) {
    return
  }

  const nextRowHeights = [...table.value.rowHeights]
  nextRowHeights[index] = height
  table.value = {
    ...table.value,
    rowHeights: nextRowHeights,
  }
  dirty.value = true
}

/**
 * 构建右键菜单项
 * @returns 右键菜单项列表
 */
const buildContextMenuItems = (): EditorContextMenuItem[] => {
  if (!table.value) {
    return [
      {
        command: 'create',
        label: '新建表格',
        icon: 'mdi:table-plus',
      },
    ]
  }

  const items: EditorContextMenuItem[] = []

  if (mergeValidation.value.canMerge) {
    items.push({
      command: 'merge-cells',
      label: '合并单元格',
      icon: 'mdi:table-merge-cells',
    })
  }

  if (activeCell.value && (activeCell.value.rowSpan > 1 || activeCell.value.colSpan > 1)) {
    items.push({
      command: 'split-cell',
      label: '拆分单元格',
      icon: 'mdi:table-split-cell',
    })
  }

  if (activeCell.value) {
    items.push(
      {
        command: 'insert-row-below',
        label: '下方插入整行',
        icon: 'mdi:table-row-plus-after',
      },
      {
        command: 'insert-column-right',
        label: '右侧插入整列',
        icon: 'mdi:table-column-plus-after',
      },
      {
        command: 'delete-row',
        label: '删除整行',
        icon: 'mdi:table-row-remove',
      },
      {
        command: 'delete-column',
        label: '删除整列',
        icon: 'mdi:table-column-remove',
      },
    )
  }

  items.push({
    command: 'rebuild',
    label: '重建表格',
    icon: 'mdi:table-refresh',
  })

  return items
}

// 处理右键菜单
const handleCanvasContextMenu = (payload: EditorCanvasContextMenuPayload) => {
  // 如果右键点击的单元格不在当前选区，则将该单元格设为当前选区
  if (table.value && payload.cellId && !selectedCellIds.value.includes(payload.cellId)) {
    collapseSelectionToCell(payload.cellId)
  }

  contextMenuRef.value?.open(
    {
      x: payload.x,
      y: payload.y,
    },
    buildContextMenuItems(),
  )
}

/**
 * 处理右键菜单命令
 * @param command 命令
 */
const handleContextCommand = (command: EditorContextMenuCommand) => {
  switch (command) {
    case 'create':
    case 'rebuild':
      openTableDialog(command)
      return

    // 合并单元格
    case 'merge-cells': {
      if (!table.value) {
        return
      }

      const validation = validateMergeSelection(table.value, selectedCellIds.value)

      if (!validation.canMerge || !validation.topLeftCell) {
        ElMessage.warning(validation.reason || '当前选区无法合并')
        return
      }

      table.value = mergeSelectedCells(table.value, selectedCellIds.value)
      collapseSelectionToCell(validation.topLeftCell.id)
      dirty.value = true
      return
    }

    // 拆分单元格
    case 'split-cell':
      if (!table.value || !activeCell.value) {
        return
      }

      table.value = splitMergedCell(table.value, activeCell.value.id)
      collapseSelectionToCell(activeCell.value.id)
      dirty.value = true
      return

    // 在下方插入整行
    case 'insert-row-below':
      if (!table.value || !activeCell.value) {
        return
      }

      table.value = insertRowBelow(
        table.value,
        activeCell.value.id,
        EDITOR_TABLE_DEFAULT_ROW_HEIGHT,
      )
      collapseSelectionToCell(activeCell.value.id)
      dirty.value = true
      return

    // 在右侧插入整列
    case 'insert-column-right':
      if (!table.value || !activeCell.value) {
        return
      }

      table.value = insertColumnRight(
        table.value,
        activeCell.value.id,
        EDITOR_TABLE_DEFAULT_COLUMN_WIDTH,
      )
      collapseSelectionToCell(activeCell.value.id)
      dirty.value = true
      return

    // 删除整行
    case 'delete-row': {
      if (!table.value || !activeCell.value) {
        return
      }

      const nextTable = deleteRow(table.value, activeCell.value.id)

      table.value = nextTable

      if (!nextTable) {
        // 如果表格为空，重置选区
        resetEditorSelection()
        dirty.value = true
        return
      }

      resetEditorSelection()
      dirty.value = true
      return
    }

    // 删除整列
    case 'delete-column': {
      if (!table.value || !activeCell.value) {
        return
      }

      const nextTable = deleteColumn(table.value, activeCell.value.id)

      table.value = nextTable

      if (!nextTable) {
        // 如果表格为空，重置选区
        resetEditorSelection()
        dirty.value = true
        return
      }

      resetEditorSelection()
      dirty.value = true
      return
    }
  }
}
</script>

<template>
  <div class="flex h-screen flex-col overflow-hidden bg-slate-100">
    <EditorHeader
      :dirty="dirty"
      :publish-loading="publishLoading"
      :save-loading="saveLoading"
      status-text="草稿"
      title="Formly 表单编辑器"
      @back="handleBack"
      @more-action="handleMoreAction"
      @preview="handlePreview"
      @publish="handlePublish"
      @save="handleSave"
    />

    <main class="flex min-h-0 flex-1 gap-4 overflow-hidden p-4 lg:p-5">
      <div class="min-h-0 w-29 shrink-0">
        <EditorComponentPalette @select-item="handleSelectItem" />
      </div>

      <div class="min-w-0 min-h-0 flex-1">
        <EditorCanvas
          :active-cell-id="activeCellId"
          :active-field-id="activeFieldId"
          :selected-cell-ids="selectedCellIds"
          :selection-anchor-cell-id="selectionAnchorCellId"
          :table="table"
          @change-selection="handleChangeSelection"
          @clear-selection="handleClearSelection"
          @context-menu="handleCanvasContextMenu"
          @place-item="handlePlaceItem"
          @resize-column="handleResizeColumn"
          @resize-row="handleResizeRow"
          @select-field="handleSelectField"
        />
      </div>

      <div class="min-h-0 w-[320px] shrink-0">
        <EditorConfigPanel
          :active-cell="activeCell"
          :active-field="activeField"
          :cell-fields="cellFields"
          @change-field-type="handleChangeFieldType"
          @remove-field="handleRemoveActiveField"
          @select-field="activeFieldId = $event"
          @update-field="handleUpdateField"
        />
      </div>
    </main>

    <EditorCreateTableDialog ref="tableDialogRef" @confirm="handleCreateTable" />

    <EditorContextMenu ref="contextMenuRef" @command="handleContextCommand" />
  </div>
</template>
