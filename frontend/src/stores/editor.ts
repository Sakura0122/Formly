import { defineStore } from 'pinia'
import { computed, ref, shallowRef } from 'vue'

import {
  EDITOR_DEFAULT_OPTIONS,
  EDITOR_TABLE_DEFAULT_COLUMN_WIDTH,
  EDITOR_TABLE_DEFAULT_ROW_HEIGHT,
  EDITOR_TABLE_LIMITS,
  EDITOR_TABLE_MIN_COLUMN_WIDTH,
} from '@/constants/editor'
import type {
  EditorCanvasDropPayload,
  EditorCanvasSelectionPayload,
  EditorCanvasTable,
  EditorCellClipboard,
  EditorComponentType,
  EditorContextMenuActionPayload,
  EditorContextMenuCommand,
  EditorContextMenuItem,
  EditorCreateTableForm,
  EditorFieldInstance,
  EditorFieldOptionChangePayload,
  EditorFieldOptionRemovePayload,
  EditorFieldOption,
  EditorFieldPatch,
  EditorHistorySnapshot,
  EditorHorizontalAlign,
  EditorPaletteItem,
  EditorRemoveFieldPayload,
  EditorResizeColumnPayload,
  EditorResizeRowPayload,
  EditorSchema,
  EditorSelectFieldPayload,
} from '@/types/editor'
import {
  createEditorTable,
  createUUID,
  deleteColumn,
  deleteRow,
  getCellById,
  insertColumnLeft,
  insertColumnRight,
  insertRowAbove,
  insertRowBelow,
  mergeSelectedCells,
  splitMergedCell,
  validateMergeSelection,
} from '@/views/editor/utils/table'
import type { FormDefinitionEditorDetail } from '@/api/form-definition/type'

/**
 * 编辑器唯一领域状态入口。
 */
export const useEditorStore = defineStore('editor', () => {
  /** 当前整张表的数据 */
  const table = shallowRef<EditorCanvasTable | null>(null)
  /** 当前激活的单元格 id */
  const activeCellId = ref('')
  /** 当前激活的组件 id */
  const activeFieldId = ref('')
  /** 当前被选中的单元格 id 列表 */
  const selectedCellIds = ref<string[]>([])
  /** 框选时的锚点单元格 */
  const selectionAnchorCellId = ref('')
  /** 页面内剪贴板，仅保存组件配置 */
  const cellClipboard = shallowRef<EditorCellClipboard | null>(null)
  /** 当前表单 ID */
  const currentFormId = ref('')
  /** 当前表单名称 */
  const formName = ref('Formly 表单编辑器')
  /** 当前发布版本 ID */
  const publishedVersionId = ref<string | null>(null)
  /** 是否存在已保存草稿 */
  const hasSavedDraft = ref(false)
  /** 是否存在未发布草稿 */
  const hasUnpublishedDraft = ref(false)
  /** 是否存在未保存改动 */
  const dirty = ref(false)
  /** 撤销历史 */
  const undoHistory = shallowRef<EditorHistorySnapshot[]>([])
  /** 重做历史 */
  const redoHistory = shallowRef<EditorHistorySnapshot[]>([])

  /** 当前激活的单元格 */
  const activeCell = computed(() => {
    return getCellById(table.value, activeCellId.value)
  })

  /** 当前激活单元格中的组件列表 */
  const cellFields = computed(() => {
    return activeCell.value?.fields ?? []
  })

  /** 当前激活的组件 */
  const activeField = computed(() => {
    return cellFields.value.find((field) => field.uuid === activeFieldId.value) ?? null
  })

  /** 当前选区是否满足合并条件 */
  const mergeValidation = computed(() => {
    return validateMergeSelection(table.value, selectedCellIds.value)
  })

  /** 是否可以撤销 */
  const canUndo = computed(() => {
    return undoHistory.value.length > 0
  })

  /** 是否可以重做 */
  const canRedo = computed(() => {
    return redoHistory.value.length > 0
  })

  /**
   * 清空当前选区与焦点组件。
   */
  const resetSelection = () => {
    activeCellId.value = ''
    activeFieldId.value = ''
    selectedCellIds.value = []
    selectionAnchorCellId.value = ''
  }

  /**
   * 单元格切换后默认聚焦到该格最后一个组件。
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
   * 应用来自画布层的选区结果。
   */
  const applySelection = (payload: EditorCanvasSelectionPayload) => {
    activeCellId.value = payload.activeCellId
    selectedCellIds.value = payload.selectedCellIds
    selectionAnchorCellId.value = payload.selectionAnchorCellId
    syncActiveFieldByCell(payload.activeCellId)
  }

  /**
   * 将多选收口成单格选中态。
   */
  const collapseSelectionToCell = (cellId: string) => {
    applySelection({
      activeCellId: cellId,
      selectedCellIds: [cellId],
      selectionAnchorCellId: cellId,
    })
  }

  /**
   * 历史记录只保存 table 快照，选区在恢复时统一清空。
   */
  const createHistorySnapshot = (): EditorHistorySnapshot => {
    return {
      table: cloneTableSnapshot(table.value),
    }
  }

  /**
   * 恢复历史快照
   * @param snapshot 历史快照
   */
  const restoreHistorySnapshot = (snapshot: EditorHistorySnapshot) => {
    table.value = cloneTableSnapshot(snapshot.table)
    resetSelection()
    dirty.value = true
  }

  /**
   * 所有会改动表格结构或字段数据的操作都统一走这里入栈。
   */
  const commitTableChange = (updater: (currentTable: EditorCanvasTable | null) => EditorCanvasTable | null) => {
    const currentTableSnapshot = cloneTableSnapshot(table.value)
    const nextTable = updater(currentTableSnapshot)

    if (nextTable === currentTableSnapshot) {
      return false
    }

    undoHistory.value = [
      ...undoHistory.value,
      {
        table: currentTableSnapshot,
      },
    ]
    redoHistory.value = []
    table.value = nextTable
    dirty.value = true

    return true
  }

  /**
   * 撤销时将当前快照压入 redo 栈。
   */
  const undo = () => {
    const snapshot = undoHistory.value.at(-1)

    if (!snapshot) {
      return false
    }

    redoHistory.value = [...redoHistory.value, createHistorySnapshot()]
    undoHistory.value = undoHistory.value.slice(0, -1)
    restoreHistorySnapshot(snapshot)

    return true
  }

  /**
   * 恢复时将当前快照重新压回 undo 栈。
   */
  const redo = () => {
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
   * 新建或重建表格后统一回到空选中态。
   */
  const createTable = (payload: EditorCreateTableForm) => {
    commitTableChange(() =>
      createEditorTable(
        payload.rows,
        payload.columns,
        EDITOR_TABLE_DEFAULT_COLUMN_WIDTH,
        EDITOR_TABLE_DEFAULT_ROW_HEIGHT,
      ),
    )
    resetSelection()
  }

  /**
   * 将当前内存态序列化为可落库的 schema。
   */
  const buildSchema = (): EditorSchema => {
    return cloneTableSnapshot(table.value)
  }

  /**
   * 用后端返回的 schema 重置编辑器现场。
   */
  const hydrateEditor = (payload: FormDefinitionEditorDetail) => {
    table.value = cloneTableSnapshot(payload.schema ?? null)
    currentFormId.value = payload.id
    formName.value = payload.name
    publishedVersionId.value = payload.publishedVersionId
    hasSavedDraft.value = Boolean(payload.schema)
    hasUnpublishedDraft.value = payload.hasUnpublishedDraft
    cellClipboard.value = null
    dirty.value = false
    undoHistory.value = []
    redoHistory.value = []
    resetSelection()
  }

  /**
   * 保存成功后同步版本指针并清理脏状态。
   */
  const markPersisted = (payload: {
    publishedVersionId: string | null
    hasUnpublishedDraft: boolean
  }) => {
    publishedVersionId.value = payload.publishedVersionId
    hasSavedDraft.value = true
    hasUnpublishedDraft.value = payload.hasUnpublishedDraft
    dirty.value = false
  }

  /**
   * 离开编辑页或切换表单时重置全部状态，避免串表。
   */
  const resetEditorSession = () => {
    table.value = null
    currentFormId.value = ''
    formName.value = 'Formly 表单编辑器'
    publishedVersionId.value = null
    hasSavedDraft.value = false
    hasUnpublishedDraft.value = false
    cellClipboard.value = null
    dirty.value = false
    undoHistory.value = []
    redoHistory.value = []
    resetSelection()
  }

  /**
   * 组件点击优先复用当前选区；没有多选时退回到当前焦点单元格。
   */
  const getSelectedPlacementCellIds = () => {
    const targetCellIds = selectedCellIds.value.length
      ? [...selectedCellIds.value]
      : activeCellId.value
        ? [activeCellId.value]
        : []

    return targetCellIds.filter((cellId) => Boolean(getCellById(table.value, cellId)))
  }

  /**
   * 拖拽命中当前多选区域时，整个选区一起追加；拖到选区外则只处理落点格。
   */
  const getDropPlacementCellIds = (cellId: string) => {
    if (selectedCellIds.value.length > 1 && selectedCellIds.value.includes(cellId)) {
      return getSelectedPlacementCellIds()
    }

    return getCellById(table.value, cellId) ? [cellId] : []
  }

  /**
   * 向目标单元格追加一个或多个新字段实例。
   */
  const appendFieldToCells = (cellIds: string[], type: EditorPaletteItem['type']) => {
    if (!table.value) {
      ElMessage.warning('请先在画布区域右键新建表格')
      return false
    }

    const targetCellIds = [...new Set(cellIds)].filter((cellId) => Boolean(getCellById(table.value, cellId)))

    if (!targetCellIds.length) {
      return false
    }

    const appendedFieldMap = new Map(
      targetCellIds.map((cellId) => {
        return [cellId, createFieldInstance(type)]
      }),
    )
    const changed = commitTableChange((currentTable) => {
      if (!currentTable) {
        return currentTable
      }

      return {
        ...currentTable,
        cells: currentTable.cells.map((cell) => {
          const nextField = appendedFieldMap.get(cell.id)

          if (!nextField) {
            return cell
          }

          return {
            ...cell,
            fields: [...cell.fields, nextField],
          }
        }),
      }
    })

    if (!changed) {
      return false
    }

    const focusCellId = targetCellIds.includes(activeCellId.value) ? activeCellId.value : (targetCellIds[0] ?? '')

    if (!focusCellId) {
      return true
    }

    if (targetCellIds.length === 1) {
      collapseSelectionToCell(focusCellId)
      activeFieldId.value = appendedFieldMap.get(focusCellId)?.uuid ?? ''
      return true
    }

    activeCellId.value = focusCellId
    selectedCellIds.value = [...targetCellIds]
    selectionAnchorCellId.value = selectionAnchorCellId.value || focusCellId
    activeFieldId.value = appendedFieldMap.get(focusCellId)?.uuid ?? ''

    return true
  }

  /**
   * 组件库点击优先按当前选区批量追加；没有多选时退回到焦点单元格。
   */
  const selectItem = (item: EditorPaletteItem) => {
    const targetCellIds = getSelectedPlacementCellIds()

    if (!targetCellIds.length) {
      ElMessage.warning('请先选择单元格')
      return
    }

    appendFieldToCells(targetCellIds, item.type)
  }

  /**
   * 放置组件
   * @param payload 放置组件参数
   */
  const placeItem = (payload: EditorCanvasDropPayload) => {
    appendFieldToCells(getDropPlacementCellIds(payload.cellId), payload.type)
  }

  /**
   * 选中组件
   */
  const selectField = ({ cellId, fieldId }: EditorSelectFieldPayload) => {
    collapseSelectionToCell(cellId)
    activeFieldId.value = fieldId
  }

  /**
   * 删除组件
   */
  const removeField = ({ cellId, fieldId }: EditorRemoveFieldPayload) => {
    const changed = commitTableChange((currentTable) =>
      replaceCell(currentTable, cellId, (cell) => {
        const nextFields = cell.fields.filter((field) => field.uuid !== fieldId)

        return {
          ...cell,
          fields: nextFields,
        }
      }),
    )

    if (!changed) {
      return
    }

    collapseSelectionToCell(cellId)
  }

  /**
   * 仅更新当前焦点字段，其他字段保持原样。
   */
  const updateField = (patch: EditorFieldPatch) => {
    const currentActiveCell = activeCell.value
    const currentActiveField = activeField.value

    if (!currentActiveCell || !currentActiveField) {
      return
    }

    commitTableChange((currentTable) =>
      replaceCell(currentTable, currentActiveCell.id, (cell) => ({
        ...cell,
        fields: cell.fields.map((field) => {
          if (field.uuid !== currentActiveField.uuid) {
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
   * 仅更新当前激活字段的某一项选项内容，组件层只传编辑意图。
   */
  const updateActiveFieldOption = ({ index, key, value }: EditorFieldOptionChangePayload) => {
    const currentActiveCell = activeCell.value
    const currentActiveField = activeField.value

    if (!currentActiveCell || !currentActiveField) {
      return
    }

    commitTableChange((currentTable) =>
      replaceCell(currentTable, currentActiveCell.id, (cell) => ({
        ...cell,
        fields: cell.fields.map((field) => {
          if (field.uuid !== currentActiveField.uuid || !field.options[index]) {
            return field
          }

          return {
            ...field,
            options: field.options.map((option, optionIndex) => {
              if (optionIndex !== index) {
                return option
              }

              return {
                ...option,
                [key]: value,
              }
            }),
          }
        }),
      })),
    )
  }

  /**
   * 为当前激活字段追加一个新选项，默认值在 store 内生成。
   */
  const addActiveFieldOption = () => {
    const currentActiveCell = activeCell.value
    const currentActiveField = activeField.value

    if (!currentActiveCell || !currentActiveField) {
      return
    }

    commitTableChange((currentTable) =>
      replaceCell(currentTable, currentActiveCell.id, (cell) => ({
        ...cell,
        fields: cell.fields.map((field) => {
          if (field.uuid !== currentActiveField.uuid) {
            return field
          }

          return {
            ...field,
            options: [...field.options, createFieldOptionDraft(field.options.length)],
          }
        }),
      })),
    )
  }

  /**
   * 删除当前激活字段的选项；最后一项会回退为默认草稿，避免空选项态。
   */
  const removeActiveFieldOption = ({ index }: EditorFieldOptionRemovePayload) => {
    const currentActiveCell = activeCell.value
    const currentActiveField = activeField.value

    if (!currentActiveCell || !currentActiveField) {
      return
    }

    commitTableChange((currentTable) =>
      replaceCell(currentTable, currentActiveCell.id, (cell) => ({
        ...cell,
        fields: cell.fields.map((field) => {
          if (field.uuid !== currentActiveField.uuid) {
            return field
          }

          const nextOptions = field.options.filter((_, optionIndex) => optionIndex !== index)

          return {
            ...field,
            options: nextOptions.length ? nextOptions : [createFieldOptionDraft(0)],
          }
        }),
      })),
    )
  }

  /**
   * 切换字段类型时保留可复用配置，其余字段回退到该类型默认值。
   */
  const changeFieldType = (type: EditorPaletteItem['type']) => {
    const currentField = activeField.value

    if (!currentField) {
      return
    }

    const defaults = createFieldDefaults(type)

    updateField({
      type,
      placeholder: defaults.placeholder,
      horizontalAlign: currentField.horizontalAlign || defaults.horizontalAlign,
      textContent: type === 'text' ? currentField.textContent || defaults.textContent : defaults.textContent,
      imageUrl: type === 'image' ? currentField.imageUrl || defaults.imageUrl : defaults.imageUrl,
      options:
        type === 'radio' || type === 'checkbox' || type === 'select'
          ? currentField.options.length
            ? cloneEditorValue(currentField.options)
            : defaults.options
          : [],
      switchActiveText:
        type === 'switch' ? currentField.switchActiveText || defaults.switchActiveText : defaults.switchActiveText,
      switchInactiveText:
        type === 'switch'
          ? currentField.switchInactiveText || defaults.switchInactiveText
          : defaults.switchInactiveText,
    })
  }

  /**
   * 删除当前激活组件
   * fieldId 组件id
   */
  const removeActiveField = (fieldId: string) => {
    const currentActiveCell = activeCell.value

    if (!currentActiveCell) {
      return
    }

    removeField({
      cellId: currentActiveCell.id,
      fieldId,
    })
  }

  /**
   * 复制源限定为当前激活单元格，避免多选复制带来结构歧义。
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
   * 粘贴优先复用当前选区；无多选时退回到活动单元格。
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
   * 深拷贝当前单元格组件，后续粘贴时再为每个目标格重新生成 uuid。
   */
  const copyActiveCell = () => {
    const targetCell = getCopySourceCell()

    if (!targetCell) {
      return false
    }

    if (!targetCell.fields.length) {
      ElMessage.warning('当前单元格没有可复制的组件')
      return false
    }

    cellClipboard.value = {
      fields: copyFieldInstances(targetCell.fields),
    }
    ElMessage.success(`已复制 ${targetCell.fields.length} 个组件`)

    return true
  }

  /**
   * 粘贴会整体替换目标单元格现有组件，但不影响表格结构和合并状态。
   */
  const pasteToActiveCell = () => {
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
            fields: copyFieldInstances(cellClipboard.value?.fields ?? [], {
              regenerateUuid: true,
            }),
          }
        }),
      }
    })

    if (!changed) {
      return false
    }

    const focusCellId = targetCellIds.includes(activeCellId.value) ? activeCellId.value : (targetCellIds[0] ?? '')

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

  /**
   * 列宽拖拽优先与右侧相邻列联动，避免表格无限扩张。
   */
  const resizeColumn = ({ index, width }: EditorResizeColumnPayload) => {
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

  /**
   * 行高为显式值，直接覆盖当前行高度。
   */
  const resizeRow = ({ index, height }: EditorResizeRowPayload) => {
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
   * 限制插入数量在 [1, max] 范围内。
   */
  const clampInsertCount = (count: number, max: number) => {
    if (max <= 0) {
      return 0
    }

    return Math.min(max, Math.max(1, Math.floor(count)))
  }

  /**
   * 构建带数量输入的菜单项。
   */
  const buildInsertMenuItem = (
    command: EditorContextMenuCommand,
    label: string,
    icon: string,
    max: number,
    unit: '行' | '列',
  ): EditorContextMenuItem => {
    return {
      command,
      label,
      icon,
      disabled: max <= 0,
      countInput: {
        min: 1,
        max,
        unit,
        defaultValue: 1,
      },
    }
  }

  /**
   * 右键菜单项由当前表格结构和选区状态共同决定。
   */
  const getContextMenuItems = (): EditorContextMenuItem[] => {
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
    const rowInsertLimit = Math.max(EDITOR_TABLE_LIMITS.maxRows - table.value.rows, 0)
    const columnInsertLimit = Math.max(EDITOR_TABLE_LIMITS.maxColumns - table.value.columns, 0)

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
        buildInsertMenuItem('insert-row-above', '上方插入', 'mdi:table-row-plus-before', rowInsertLimit, '行'),
        buildInsertMenuItem('insert-row-below', '下方插入', 'mdi:table-row-plus-after', rowInsertLimit, '行'),
        buildInsertMenuItem('insert-column-left', '左侧插入', 'mdi:table-column-plus-before', columnInsertLimit, '列'),
        buildInsertMenuItem(
          'insert-column-right',
          '右侧插入',
          'mdi:table-column-plus-after',
          columnInsertLimit,
          '列',
        ),
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

  /**
   * 集中处理右键菜单命令。
   */
  const executeContextCommand = ({ command, count }: EditorContextMenuActionPayload) => {
    switch (command) {
      case 'create':
      case 'rebuild':
        return

      case 'merge-cells': {
        if (!table.value) {
          return
        }

        const currentMergeValidation = mergeValidation.value
        const nextTopLeftCellId = currentMergeValidation.topLeftCell?.id ?? ''

        if (!currentMergeValidation.canMerge || !nextTopLeftCellId) {
          ElMessage.warning(currentMergeValidation.reason || '当前选区无法合并')
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

        collapseSelectionToCell(nextTopLeftCellId)
        return
      }

      case 'split-cell': {
        const currentActiveCell = activeCell.value

        if (!table.value || !currentActiveCell) {
          return
        }

        const changed = commitTableChange((currentTable) => {
          if (!currentTable) {
            return currentTable
          }

          return splitMergedCell(currentTable, currentActiveCell.id)
        })

        if (!changed) {
          return
        }

        collapseSelectionToCell(currentActiveCell.id)
        return
      }

      case 'insert-row-above':
      case 'insert-row-below': {
        const currentActiveCell = activeCell.value

        if (!table.value || !currentActiveCell) {
          return
        }

        const safeCount = clampInsertCount(count, EDITOR_TABLE_LIMITS.maxRows - table.value.rows)

        if (!safeCount) {
          ElMessage.warning('已达到最大行数')
          return
        }

        const changed = commitTableChange((currentTable) => {
          if (!currentTable) {
            return currentTable
          }

          return command === 'insert-row-above'
            ? insertRowAbove(currentTable, currentActiveCell.id, EDITOR_TABLE_DEFAULT_ROW_HEIGHT, safeCount)
            : insertRowBelow(currentTable, currentActiveCell.id, EDITOR_TABLE_DEFAULT_ROW_HEIGHT, safeCount)
        })

        if (!changed) {
          return
        }

        collapseSelectionToCell(currentActiveCell.id)
        return
      }

      case 'insert-column-left':
      case 'insert-column-right': {
        const currentActiveCell = activeCell.value

        if (!table.value || !currentActiveCell) {
          return
        }

        const safeCount = clampInsertCount(count, EDITOR_TABLE_LIMITS.maxColumns - table.value.columns)

        if (!safeCount) {
          ElMessage.warning('已达到最大列数')
          return
        }

        const changed = commitTableChange((currentTable) => {
          if (!currentTable) {
            return currentTable
          }

          return command === 'insert-column-left'
            ? insertColumnLeft(currentTable, currentActiveCell.id, EDITOR_TABLE_DEFAULT_COLUMN_WIDTH, safeCount)
            : insertColumnRight(currentTable, currentActiveCell.id, EDITOR_TABLE_DEFAULT_COLUMN_WIDTH, safeCount)
        })

        if (!changed) {
          return
        }

        collapseSelectionToCell(currentActiveCell.id)
        return
      }

      case 'delete-row': {
        const currentActiveCell = activeCell.value

        if (!table.value || !currentActiveCell) {
          return
        }

        const changed = commitTableChange((currentTable) => {
          if (!currentTable) {
            return currentTable
          }

          return deleteRow(currentTable, currentActiveCell.id)
        })

        if (!changed) {
          return
        }

        resetSelection()
        return
      }

      case 'delete-column': {
        const currentActiveCell = activeCell.value

        if (!table.value || !currentActiveCell) {
          return
        }

        const changed = commitTableChange((currentTable) => {
          if (!currentTable) {
            return currentTable
          }

          return deleteColumn(currentTable, currentActiveCell.id)
        })

        if (!changed) {
          return
        }

        resetSelection()
      }
    }
  }

  return {
    activeCell,
    activeCellId,
    activeField,
    activeFieldId,
    addActiveFieldOption,
    applySelection,
    canRedo,
    canUndo,
    cellClipboard,
    cellFields,
    changeFieldType,
    collapseSelectionToCell,
    commitTableChange,
    copyActiveCell,
    createTable,
    currentFormId,
    dirty,
    executeContextCommand,
    formName,
    hasSavedDraft,
    hasUnpublishedDraft,
    hydrateEditor,
    getContextMenuItems,
    buildSchema,
    markPersisted,
    mergeValidation,
    pasteToActiveCell,
    placeItem,
    publishedVersionId,
    redo,
    removeActiveField,
    removeField,
    removeActiveFieldOption,
    resetEditorSession,
    resetSelection,
    resizeColumn,
    resizeRow,
    selectField,
    selectedCellIds,
    selectionAnchorCellId,
    selectItem,
    syncActiveFieldByCell,
    table,
    undo,
    updateActiveFieldOption,
    updateField,
  }
})

interface EditorFieldDefaults {
  placeholder: string
  horizontalAlign: EditorHorizontalAlign
  textContent: string
  imageUrl: string
  options: EditorFieldOption[]
  switchActiveText: string
  switchInactiveText: string
}

const cloneEditorValue = <T>(value: T): T => {
  return structuredClone(value)
}

const createFieldOptionDraft = (index: number): EditorFieldOption => {
  return {
    label: `选项${index + 1}`,
    value: `option_${index + 1}`,
  }
}

/**
 * 克隆字段实例，可选地重新生成 uuid 用于复制粘贴。
 */
const copyFieldInstances = (
  fields: EditorFieldInstance[],
  options: {
    regenerateUuid?: boolean
  } = {},
) => {
  const nextFields = cloneEditorValue(fields)

  if (!options.regenerateUuid) {
    return nextFields
  }

  return nextFields.map((field) => ({
    ...field,
    uuid: createUUID(),
  }))
}

/**
 * 按组件类型生成默认字段配置。
 */
const createFieldDefaults = (type: EditorComponentType): EditorFieldDefaults => {
  const options = ['radio', 'checkbox', 'select'].includes(type) ? cloneEditorValue(EDITOR_DEFAULT_OPTIONS) : []

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
    horizontalAlign: 'center',
    textContent: type === 'text' ? '固定文字内容' : '',
    imageUrl: '',
    options,
    switchActiveText: '开启',
    switchInactiveText: '关闭',
  }
}

/**
 * 创建一个可直接挂到单元格内的新字段实例。
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
 * 历史记录使用的 table 深拷贝，避免响应式对象直接入栈。
 */
const cloneTableSnapshot = (currentTable: EditorCanvasTable | null): EditorCanvasTable | null => {
  if (!currentTable) {
    return null
  }

  return cloneEditorValue(currentTable)
}

/**
 * 在不可变更新风格下替换指定单元格。
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
