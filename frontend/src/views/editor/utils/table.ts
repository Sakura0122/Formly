import type {
  EditorCanvasCell,
  EditorCanvasSelectionPayload,
  EditorCanvasTable,
} from '@/types/editor'

export const createUUID = (num: number = 8) => {
  return crypto.randomUUID().replace(/-/g, '').slice(0, num)
}

const createCell = (row: number, col: number): EditorCanvasCell => {
  return {
    id: `cell_${createUUID()}`,
    row,
    col,
    fields: [],
    rowSpan: 1,
    colSpan: 1,
    merged: false,
    mergeParentId: '',
  }
}

export const createEditorTable = (
  rows: number,
  columns: number,
  defaultColumnWidth: number,
  defaultRowHeight: number,
): EditorCanvasTable => {
  const cells: EditorCanvasCell[] = []

  for (let row = 1; row <= rows; row += 1) {
    for (let col = 1; col <= columns; col += 1) {
      cells.push(createCell(row, col))
    }
  }

  return {
    rows,
    columns,
    cells,
    columnWidths: Array.from({ length: columns }, () => defaultColumnWidth),
    rowHeights: Array.from({ length: rows }, () => defaultRowHeight),
  }
}

export const sortCells = (cells: EditorCanvasCell[]) => {
  return [...cells].sort((left, right) => {
    if (left.row !== right.row) {
      return left.row - right.row
    }

    return left.col - right.col
  })
}

export const getCellById = (table: EditorCanvasTable | null, cellId: string) => {
  return table?.cells.find((cell) => cell.id === cellId) ?? null
}

export const getCellAt = (table: EditorCanvasTable | null, row: number, col: number) => {
  return table?.cells.find((cell) => cell.row === row && cell.col === col) ?? null
}

export const getVisibleCells = (table: EditorCanvasTable | null) => {
  return (table?.cells ?? []).filter((cell) => !cell.merged)
}

/**
 * 获取单元格的范围
 * @param cell 单元格
 * @returns rowStart 起始行
 * @returns rowEnd 结束行
 * @returns colStart 起始列
 * @returns colEnd 结束列
 */
export const getCellRange = (cell: EditorCanvasCell) => {
  return {
    rowStart: cell.row,
    rowEnd: cell.row + cell.rowSpan - 1,
    colStart: cell.col,
    colEnd: cell.col + cell.colSpan - 1,
  }
}

/**
 * 判断单元格是否与范围相交
 * @param cell 单元格
 * @param bounds 范围
 * @returns boolean
 */
const intersectsRange = (
  cell: EditorCanvasCell,
  bounds: {
    rowStart: number
    rowEnd: number
    colStart: number
    colEnd: number
  },
) => {
  const range = getCellRange(cell)

  return !(
    range.rowEnd < bounds.rowStart ||
    range.rowStart > bounds.rowEnd ||
    range.colEnd < bounds.colStart ||
    range.colStart > bounds.colEnd
  )
}

/**
 * 选中了哪些单元格
 * @param table 表格数据
 * @param anchorCellId 起点格
 * @param targetCellId 终点格
 * @returns activeCellId 当前激活格是谁
 * @returns selectionAnchorCellId 起点格是谁
 * @returns selectedCellIds 选中的单元格id列表
 */
export const buildSelectionPayload = (
  table: EditorCanvasTable | null,
  anchorCellId: string,
  targetCellId: string,
): EditorCanvasSelectionPayload => {
  // 先找到起点和终点对应的 cell
  const anchorCell = getCellById(table, anchorCellId)
  const targetCell = getCellById(table, targetCellId)

  if (!table || !anchorCell || !targetCell) {
    return {
      activeCellId: targetCellId,
      selectedCellIds: targetCellId ? [targetCellId] : [],
      selectionAnchorCellId: anchorCellId || targetCellId,
    }
  }

  // 计算起点和终点的范围
  const anchorRange = getCellRange(anchorCell)
  const targetRange = getCellRange(targetCell)
  // 计算范围的边界
  const bounds = {
    rowStart: Math.min(anchorRange.rowStart, targetRange.rowStart),
    rowEnd: Math.max(anchorRange.rowEnd, targetRange.rowEnd),
    colStart: Math.min(anchorRange.colStart, targetRange.colStart),
    colEnd: Math.max(anchorRange.colEnd, targetRange.colEnd),
  }

  return {
    activeCellId: targetCell.id,
    selectionAnchorCellId: anchorCell.id,
    selectedCellIds: getVisibleCells(table)
      .filter((cell) => intersectsRange(cell, bounds))
      .map((cell) => cell.id),
  }
}

const buildSelectionBounds = (table: EditorCanvasTable, selectedCellIds: string[]) => {
  const selectedCells = getVisibleCells(table).filter((cell) => selectedCellIds.includes(cell.id))

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
}

/**
 * 验证合并单元格
 * @param table 表格数据
 * @param selectedCellIds 选中的单元格id列表
 * @returns canMerge 是否可以合并
 * @returns reason 原因
 * @returns bounds 范围
 * @returns topLeftCell 左上角单元格
 */
export const validateMergeSelection = (
  table: EditorCanvasTable | null,
  selectedCellIds: string[],
) => {
  if (!table || selectedCellIds.length < 2) {
    return {
      canMerge: false,
      reason: '请先选择至少两个单元格',
      bounds: null,
      topLeftCell: null,
    }
  }

  const selectedCells = getVisibleCells(table).filter((cell) => selectedCellIds.includes(cell.id))
  const bounds = buildSelectionBounds(table, selectedCellIds)

  if (!selectedCells.length || !bounds) {
    return {
      canMerge: false,
      reason: '请选择有效的单元格',
      bounds: null,
      topLeftCell: null,
    }
  }

  const intersectedVisibleCells = getVisibleCells(table).filter((cell) =>
    intersectsRange(cell, bounds),
  )

  if (intersectedVisibleCells.length !== selectedCells.length) {
    return {
      canMerge: false,
      reason: '当前选区包含已合并区域或非规则矩形',
      bounds,
      topLeftCell: null,
    }
  }

  if (selectedCells.some((cell) => cell.rowSpan > 1 || cell.colSpan > 1)) {
    return {
      canMerge: false,
      reason: '暂不支持在已合并区域上继续合并',
      bounds,
      topLeftCell: null,
    }
  }

  const cellsWithContent = selectedCells.filter((cell) => cell.fields.length > 0)

  if (cellsWithContent.length > 1) {
    return {
      canMerge: false,
      reason: '多个有内容的单元格不能直接合并',
      bounds,
      topLeftCell: null,
    }
  }

  const topLeftCell = [...selectedCells].sort((left, right) => {
    if (left.row !== right.row) {
      return left.row - right.row
    }

    return left.col - right.col
  })[0]

  return {
    canMerge: true,
    reason: '',
    bounds,
    topLeftCell,
  }
}

/**
 * 合并单元格
 * @param table 表格数据
 * @param selectedCellIds 选中的单元格id列表
 * @returns 合并后的表格数据
 */
export const mergeSelectedCells = (table: EditorCanvasTable, selectedCellIds: string[]) => {
  const validation = validateMergeSelection(table, selectedCellIds)

  if (!validation.canMerge || !validation.bounds || !validation.topLeftCell) {
    return table
  }

  const topLeftCell = validation.topLeftCell
  const selectedCells = getVisibleCells(table).filter((cell) => selectedCellIds.includes(cell.id))
  const contentSource = selectedCells.find((cell) => cell.fields.length > 0)
  const nextCells = table.cells.map((cell) => {
    const inBounds =
      cell.row >= validation.bounds.rowStart &&
      cell.row <= validation.bounds.rowEnd &&
      cell.col >= validation.bounds.colStart &&
      cell.col <= validation.bounds.colEnd

    // 当前这个 cell 在不在这次要处理的矩形范围里？
    if (!inBounds) {
      return cell
    }

    if (cell.id === topLeftCell.id) {
      return {
        ...cell,
        rowSpan: validation.bounds.rowEnd - validation.bounds.rowStart + 1,
        colSpan: validation.bounds.colEnd - validation.bounds.colStart + 1,
        merged: false,
        mergeParentId: '',
        fields: contentSource && contentSource.id !== cell.id ? contentSource.fields : cell.fields,
      }
    }

    return {
      ...cell,
      fields: [],
      rowSpan: 1,
      colSpan: 1,
      merged: true,
      mergeParentId: topLeftCell.id,
    }
  })

  return {
    ...table,
    cells: sortCells(nextCells),
  }
}

export const splitMergedCell = (table: EditorCanvasTable, cellId: string) => {
  const targetCell = getCellById(table, cellId)

  if (!targetCell || targetCell.merged || (targetCell.rowSpan === 1 && targetCell.colSpan === 1)) {
    return table
  }

  const bounds = getCellRange(targetCell)

  return {
    ...table,
    cells: sortCells(
      table.cells.map((cell) => {
        const inBounds =
          cell.row >= bounds.rowStart &&
          cell.row <= bounds.rowEnd &&
          cell.col >= bounds.colStart &&
          cell.col <= bounds.colEnd

        if (!inBounds) {
          return cell
        }

        return {
          ...cell,
          rowSpan: 1,
          colSpan: 1,
          merged: false,
          mergeParentId: '',
        }
      }),
    ),
  }
}

/**
 * 在下方插入整行
 * @param table 表格数据
 * @param cellId 单元格id
 * @param defaultRowHeight 默认行高
 * @returns 插入后的表格数据
 */
export const insertRowBelow = (
  table: EditorCanvasTable,
  cellId: string,
  defaultRowHeight: number,
) => {
  const activeCell = getCellById(table, cellId)

  if (!activeCell) {
    return table
  }

  const insertedRow = activeCell.row + 1
  // 找出哪些合并主格需要被纵向扩张
  const expandableMasters = getVisibleCells(table).filter((cell) => {
    const range = getCellRange(cell)

    return range.rowStart <= activeCell.row && range.rowEnd > activeCell.row
  })

  // 如果某个主格从第 2 行跨到第 3 行
  // 你现在在第 2 行下面插一行
  // 那这个主格必须从 rowSpan = 2 变成 rowSpan = 3
  const shiftedCells = table.cells.map((cell) => {
    if (cell.row >= insertedRow) {
      return {
        ...cell,
        row: cell.row + 1,
      }
    }

    return cell
  })

  const expandedIds = new Set(expandableMasters.map((cell) => cell.id))
  const nextCells = shiftedCells.map((cell) => {
    if (!expandedIds.has(cell.id)) {
      return cell
    }

    return {
      ...cell,
      rowSpan: cell.rowSpan + 1,
    }
  })

  for (let col = 1; col <= table.columns; col += 1) {
    const owner = expandableMasters.find((cell) => {
      const range = getCellRange(cell)
      return range.colStart <= col && range.colEnd >= col
    })

    nextCells.push(
      owner
        ? {
            ...createCell(insertedRow, col),
            merged: true,
            mergeParentId: owner.id,
          }
        : createCell(insertedRow, col),
    )
  }

  const nextRowHeights = [...table.rowHeights]
  nextRowHeights.splice(insertedRow - 1, 0, defaultRowHeight)

  return {
    ...table,
    rows: table.rows + 1,
    rowHeights: nextRowHeights,
    cells: sortCells(nextCells),
  }
}

/**
 * 在右侧插入整列
 * @param table 表格数据
 * @param cellId 单元格id
 * @param defaultColumnWidth 默认列宽
 * @returns 插入后的表格数据
 */
export const insertColumnRight = (
  table: EditorCanvasTable,
  cellId: string,
  defaultColumnWidth: number,
) => {
  const activeCell = getCellById(table, cellId)

  if (!activeCell) {
    return table
  }

  const insertedColumn = activeCell.col + 1
  const expandableMasters = getVisibleCells(table).filter((cell) => {
    const range = getCellRange(cell)

    return range.colStart <= activeCell.col && range.colEnd > activeCell.col
  })

  const shiftedCells = table.cells.map((cell) => {
    if (cell.col >= insertedColumn) {
      return {
        ...cell,
        col: cell.col + 1,
      }
    }

    return cell
  })

  const expandedIds = new Set(expandableMasters.map((cell) => cell.id))
  const nextCells = shiftedCells.map((cell) => {
    if (!expandedIds.has(cell.id)) {
      return cell
    }

    return {
      ...cell,
      colSpan: cell.colSpan + 1,
    }
  })

  for (let row = 1; row <= table.rows; row += 1) {
    const owner = expandableMasters.find((cell) => {
      const range = getCellRange(cell)
      return range.rowStart <= row && range.rowEnd >= row
    })

    nextCells.push(
      owner
        ? {
            ...createCell(row, insertedColumn),
            merged: true,
            mergeParentId: owner.id,
          }
        : createCell(row, insertedColumn),
    )
  }

  const nextColumnWidths = [...table.columnWidths]
  nextColumnWidths.splice(insertedColumn - 1, 0, defaultColumnWidth)

  return {
    ...table,
    columns: table.columns + 1,
    columnWidths: nextColumnWidths,
    cells: sortCells(nextCells),
  }
}

export const getFirstVisibleCell = (table: EditorCanvasTable | null) => {
  return getVisibleCells(table)[0] ?? null
}
