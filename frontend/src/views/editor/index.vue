<script setup lang="ts">
import { useEventListener } from '@vueuse/core'
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
  EditorCellClipboard,
  EditorHistorySnapshot,
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
/** 单元格剪贴板 */
const cellClipboard = ref<EditorCellClipboard | null>(null)
const undoHistory = ref<EditorHistorySnapshot[]>([])
const redoHistory = ref<EditorHistorySnapshot[]>([])

/**
 * 克隆选项
 * @param options 选项
 */
const cloneOptions = (options: EditorFieldOption[]) => {
  return options.map((option) => ({
    ...option,
  }))
}

/**
 * 深度克隆组件实例
 * @param fields 组件实例列表
 * @param options 选项
 */
const cloneFieldInstances = (
  fields: EditorFieldInstance[],
  options: {
    regenerateUuid?: boolean
  } = {},
) => {
  return fields.map((field) => ({
    ...field,
    uuid: options.regenerateUuid ? createUUID() : field.uuid,
    options: cloneOptions(field.options),
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

const canUndo = computed(() => {
  return undoHistory.value.length > 0
})

const canRedo = computed(() => {
  return redoHistory.value.length > 0
})

const cloneFieldOption = (option: EditorFieldOption) => {
  return {
    label: option.label,
    value: option.value,
  }
}

const cloneFieldSnapshot = (field: EditorFieldInstance): EditorFieldInstance => {
  return {
    uuid: field.uuid,
    type: field.type,
    placeholder: field.placeholder,
    required: field.required,
    helpText: field.helpText,
    horizontalAlign: field.horizontalAlign,
    textContent: field.textContent,
    imageUrl: field.imageUrl,
    options: field.options.map((option) => cloneFieldOption(option)),
    switchActiveText: field.switchActiveText,
    switchInactiveText: field.switchInactiveText,
  }
}

const cloneTableSnapshot = (currentTable: EditorCanvasTable | null): EditorCanvasTable | null => {
  if (!currentTable) {
    return null
  }

  return {
    rows: currentTable.rows,
    columns: currentTable.columns,
    cells: currentTable.cells.map((cell) => ({
      id: cell.id,
      row: cell.row,
      col: cell.col,
      fields: cell.fields.map((field) => cloneFieldSnapshot(field)),
      rowSpan: cell.rowSpan,
      colSpan: cell.colSpan,
      merged: cell.merged,
      mergeParentId: cell.mergeParentId,
    })),
    columnWidths: [...currentTable.columnWidths],
    rowHeights: [...currentTable.rowHeights],
  }
}

const createHistorySnapshot = (): EditorHistorySnapshot => {
  return {
    table: cloneTableSnapshot(table.value),
  }
}

const restoreHistorySnapshot = (snapshot: EditorHistorySnapshot) => {
  table.value = cloneTableSnapshot(snapshot.table)
  resetEditorSelection()
  dirty.value = true
}

/**
 * 替换单元格
 * @param currentTable 当前表格
 * @param cellId 单元格id
 * @param updater 单元格更新器
 */
const replaceCell = (
  currentTable: EditorCanvasTable | null,
  cellId: string,
  updater: (cell: EditorCanvasTable['cells'][number]) => EditorCanvasTable['cells'][number],
) => {
  if (!currentTable) {
    return currentTable
  }

  return {
    ...currentTable,
    cells: currentTable.cells.map((cell) => {
      if (cell.id !== cellId) {
        return cell
      }

      return updater(cell)
    }),
  }
}

const commitTableChange = (
  updater: (currentTable: EditorCanvasTable | null) => EditorCanvasTable | null,
) => {
  const nextTable = updater(table.value)

  if (nextTable === table.value) {
    return false
  }

  undoHistory.value = [...undoHistory.value, createHistorySnapshot()]
  redoHistory.value = []
  table.value = nextTable
  dirty.value = true

  return true
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
  if (action === 'shortcut') {
    ElMessage.info(
      '已支持快捷键：Ctrl/Cmd+C 复制单元格组件，Ctrl/Cmd+V 粘贴单元格组件，Ctrl/Cmd+Z 撤销，Ctrl/Cmd+Y 恢复',
    )
    return
  }

  const currentAction = EDITOR_HEADER_ACTION_OPTIONS.find((item) => item.key === action)
  ElMessage.info(`更多操作占位：${currentAction?.label ?? action}`)
}

/**
 * 处理创建表格
 * @param payload 表格创建表单
 */
const handleCreateTable = (payload: EditorCreateTableForm) => {
  commitTableChange(() =>
    createEditorTable(
      payload.rows,
      payload.columns,
      EDITOR_TABLE_DEFAULT_COLUMN_WIDTH,
      EDITOR_TABLE_DEFAULT_ROW_HEIGHT,
    ),
  )
  resetEditorSelection()
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

  const changed = commitTableChange((currentTable) =>
    replaceCell(currentTable, cellId, (cell) => ({
      ...cell,
      fields: [...cell.fields, nextField],
    })),
  )

  if (!changed) {
    return
  }

  collapseSelectionToCell(cellId)
  activeFieldId.value = nextField.uuid
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

  const changed = commitTableChange((currentTable) =>
    replaceCell(currentTable, cellId, (cell) => ({
      ...cell,
      fields: nextFields,
    })),
  )

  if (!changed) {
    return
  }

  collapseSelectionToCell(cellId)
  activeFieldId.value = nextFields.at(-1)?.uuid ?? ''
}

/**
 * 更新组件
 * @param patch 组件更新
 */
const handleUpdateField = (patch: EditorFieldPatch) => {
  if (!activeCell.value || !activeField.value) {
    return
  }

  commitTableChange((currentTable) =>
    replaceCell(currentTable, activeCell.value!.id, (cell) => ({
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
    })),
  )
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

/**
 * 获取复制源单元格
 * @returns 复制源单元格
 */
const getCopySourceCell = () => {
  if (!table.value) {
    ElMessage.warning('请先在画布区域右键新建表格')
    return null
  }

  if (selectedCellIds.value.length > 1) {
    ElMessage.warning('当前仅支持复制单个单元格')
    return null
  }

  if (!activeCell.value) {
    ElMessage.warning('请先选择要复制的单元格')
    return null
  }

  return activeCell.value
}

/**
 * 获取粘贴目标单元格id列表
 * @returns 目标单元格id列表
 */
const getPasteTargetCellIds = () => {
  if (!table.value) {
    ElMessage.warning('请先在画布区域右键新建表格')
    return []
  }

  if (selectedCellIds.value.length) {
    return [...selectedCellIds.value]
  }

  if (activeCell.value) {
    return [activeCell.value.id]
  }

  ElMessage.warning('请先选择要粘贴的单元格')
  return []
}

/**
 * 处理复制当前单元格组件
 * @returns 是否成功
 */
const handleCopyActiveCell = () => {
  const targetCell = getCopySourceCell()

  if (!targetCell) {
    return false
  }

  if (!targetCell.fields.length) {
    ElMessage.warning('当前单元格没有可复制的组件')
    return false
  }

  cellClipboard.value = {
    fields: cloneFieldInstances(targetCell.fields),
  }
  ElMessage.success(`已复制 ${targetCell.fields.length} 个组件`)

  return true
}

/**
 * 处理粘贴当前单元格组件
 * @returns 是否成功
 */
const handlePasteToActiveCell = () => {
  const targetCellIds = getPasteTargetCellIds()

  if (!targetCellIds.length) {
    return false
  }

  if (!cellClipboard.value?.fields.length) {
    ElMessage.warning('请先复制单元格组件')
    return false
  }

  if (!table.value) {
    return false
  }

  const targetCellIdSet = new Set(targetCellIds)

  const changed = commitTableChange((currentTable) => {
    if (!currentTable) {
      return currentTable
    }

    return {
      ...currentTable,
      cells: currentTable.cells.map((cell) => {
        if (!targetCellIdSet.has(cell.id)) {
          return cell
        }

        return {
          ...cell,
          fields: cloneFieldInstances(cellClipboard.value?.fields ?? [], {
            regenerateUuid: true,
          }),
        }
      }),
    }
  })

  if (!changed) {
    return false
  }

  const focusCellId = targetCellIds.includes(activeCellId.value)
    ? activeCellId.value
    : (targetCellIds[0] ?? '')

  if (!focusCellId) {
    return false
  }

  if (targetCellIds.length === 1) {
    collapseSelectionToCell(focusCellId)
  } else {
    activeCellId.value = focusCellId
    selectionAnchorCellId.value = selectionAnchorCellId.value || focusCellId
    syncActiveFieldByCell(focusCellId)
  }

  return true
}

const handleUndo = () => {
  const snapshot = undoHistory.value.at(-1)

  if (!snapshot) {
    return false
  }

  redoHistory.value = [...redoHistory.value, createHistorySnapshot()]
  undoHistory.value = undoHistory.value.slice(0, -1)
  restoreHistorySnapshot(snapshot)

  return true
}

const handleRedo = () => {
  const snapshot = redoHistory.value.at(-1)

  if (!snapshot) {
    return false
  }

  undoHistory.value = [...undoHistory.value, createHistorySnapshot()]
  redoHistory.value = redoHistory.value.slice(0, -1)
  restoreHistorySnapshot(snapshot)

  return true
}

/**
 * 判断目标是否为可编辑元素
 * @param target 目标元素
 */
const isEditableTarget = (target: EventTarget | null) => {
  return target instanceof HTMLElement
    ? Boolean(target.closest('input, textarea, select, [contenteditable="true"]'))
    : false
}

const handleWindowKeydown = (event: KeyboardEvent) => {
  // 过滤输入法状态、已阻止默认行为、非 Cmd/Ctrl 组合键、Shift/Alt 存在、可编辑元素
  if (
    event.isComposing ||
    event.defaultPrevented ||
    !(event.metaKey || event.ctrlKey) ||
    event.shiftKey ||
    event.altKey ||
    isEditableTarget(event.target)
  ) {
    return
  }

  const key = event.key.toLowerCase()

  if (key === 'c') {
    if (handleCopyActiveCell()) {
      event.preventDefault()
    }

    return
  }

  if (key === 'v') {
    if (handlePasteToActiveCell()) {
      event.preventDefault()
    }

    return
  }

  if (key === 'z') {
    if (handleUndo()) {
      event.preventDefault()
    }

    return
  }

  if (key === 'y' && handleRedo()) {
    event.preventDefault()
  }
}

useEventListener(window, 'keydown', handleWindowKeydown)

const handleResizeColumn = ({ index, width }: EditorResizeColumnPayload) => {
  commitTableChange((currentTable) => {
    if (!currentTable) {
      return currentTable
    }

    const nextColumnWidths = [...currentTable.columnWidths]
    const currentWidth = nextColumnWidths[index]
    const nextWidth = nextColumnWidths[index + 1]

    if (currentWidth === undefined) {
      return currentTable
    }

    if (nextWidth === undefined) {
      if (currentWidth === width) {
        return currentTable
      }

      nextColumnWidths[index] = width
    } else {
      const delta = width - currentWidth
      const shrinkableDelta = Math.min(delta, nextWidth - EDITOR_TABLE_MIN_COLUMN_WIDTH)
      const growableDelta = Math.max(delta, -(currentWidth - EDITOR_TABLE_MIN_COLUMN_WIDTH))
      const appliedDelta = delta >= 0 ? Math.max(0, shrinkableDelta) : Math.min(0, growableDelta)

      if (appliedDelta === 0) {
        return currentTable
      }

      nextColumnWidths[index] = currentWidth + appliedDelta
      nextColumnWidths[index + 1] = nextWidth - appliedDelta
    }

    return {
      ...currentTable,
      columnWidths: nextColumnWidths,
    }
  })
}

const handleResizeRow = ({ index, height }: EditorResizeRowPayload) => {
  commitTableChange((currentTable) => {
    if (!currentTable) {
      return currentTable
    }

    const nextRowHeights = [...currentTable.rowHeights]

    if (nextRowHeights[index] === height) {
      return currentTable
    }

    nextRowHeights[index] = height

    return {
      ...currentTable,
      rowHeights: nextRowHeights,
    }
  })
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

      const changed = commitTableChange((currentTable) => {
        if (!currentTable) {
          return currentTable
        }

        return mergeSelectedCells(currentTable, selectedCellIds.value)
      })

      if (!changed) {
        return
      }

      collapseSelectionToCell(validation.topLeftCell.id)
      return
    }

    // 拆分单元格
    case 'split-cell': {
      if (!table.value || !activeCell.value) {
        return
      }

      const changed = commitTableChange((currentTable) => {
        if (!currentTable) {
          return currentTable
        }

        return splitMergedCell(currentTable, activeCell.value!.id)
      })

      if (!changed) {
        return
      }

      collapseSelectionToCell(activeCell.value.id)
      return
    }

    // 在下方插入整行
    case 'insert-row-below': {
      if (!table.value || !activeCell.value) {
        return
      }

      const changed = commitTableChange((currentTable) => {
        if (!currentTable) {
          return currentTable
        }

        return insertRowBelow(currentTable, activeCell.value!.id, EDITOR_TABLE_DEFAULT_ROW_HEIGHT)
      })

      if (!changed) {
        return
      }

      collapseSelectionToCell(activeCell.value.id)
      return
    }

    // 在右侧插入整列
    case 'insert-column-right': {
      if (!table.value || !activeCell.value) {
        return
      }

      const changed = commitTableChange((currentTable) => {
        if (!currentTable) {
          return currentTable
        }

        return insertColumnRight(
          currentTable,
          activeCell.value!.id,
          EDITOR_TABLE_DEFAULT_COLUMN_WIDTH,
        )
      })

      if (!changed) {
        return
      }

      collapseSelectionToCell(activeCell.value.id)
      return
    }

    // 删除整行
    case 'delete-row': {
      if (!table.value || !activeCell.value) {
        return
      }

      const changed = commitTableChange((currentTable) => {
        if (!currentTable) {
          return currentTable
        }

        return deleteRow(currentTable, activeCell.value!.id)
      })

      if (!changed) {
        return
      }

      resetEditorSelection()
      return
    }

    // 删除整列
    case 'delete-column': {
      if (!table.value || !activeCell.value) {
        return
      }

      const changed = commitTableChange((currentTable) => {
        if (!currentTable) {
          return currentTable
        }

        return deleteColumn(currentTable, activeCell.value!.id)
      })

      if (!changed) {
        return
      }

      resetEditorSelection()
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
      :redo-disabled="!canRedo"
      :save-loading="saveLoading"
      :undo-disabled="!canUndo"
      status-text="草稿"
      title="Formly 表单编辑器"
      @back="handleBack"
      @more-action="handleMoreAction"
      @preview="handlePreview"
      @publish="handlePublish"
      @redo="handleRedo"
      @save="handleSave"
      @undo="handleUndo"
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
