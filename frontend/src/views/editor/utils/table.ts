import type { EditorCanvasCell, EditorCanvasSelectionPayload, EditorCanvasTable } from '@/types/editor'

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

/**
 * 创建表格
 * @param rows 表格行数
 * @param columns 表格列数
 * @param defaultColumnWidth 默认列宽
 * @param defaultRowHeight 默认行高
 * @returns 表格
 */
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

/**
 * 排序单元格
 * @param cells 单元格列表
 * @returns 排序后的单元格列表
 */
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
export const validateMergeSelection = (table: EditorCanvasTable | null, selectedCellIds: string[]) => {
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

  const intersectedVisibleCells = getVisibleCells(table).filter((cell) => intersectsRange(cell, bounds))

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

/**
 * 拆分单元格
 * @param table 表格数据
 * @param cellId 单元格id
 * @returns 拆分后的表格数据
 */
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

const createCellKey = (row: number, col: number) => {
  return `${row}:${col}`
}

/**
 * 把“要删除的行号/列号”整理成一个合法、干净、可用的数组
 * @param lineNumbers 要删除的行号/列号
 * @param max 表格的最大行号/列号
 * @returns 整理后的行号/列号数组
 */
const normalizeLineNumbers = (lineNumbers: number[], max: number) => {
  return [...new Set(lineNumbers.map((lineNumber) => Math.floor(lineNumber)).filter((lineNumber) => lineNumber >= 1 && lineNumber <= max))]
    .sort((left, right) => left - right)
}

/**
 * 算“在当前这一行/列之前，一共删了多少行/列”。
 * @param lineNumbers 要删除的行号/列号
 * @param currentLine 当前行号/列号
 * @returns
 */
const countDeletedBefore = (lineNumbers: number[], currentLine: number) => {
  return lineNumbers.filter((lineNumber) => lineNumber < currentLine).length
}

/**
 * 算“一个单元格自己覆盖的范围里，被删掉了多少行/列”。
 * @param lineNumbers 要删除的行号/列号
 * @param startLine 开始行号/列号
 * @param endLine 结束行号/列号
 * @returns
 */
const countDeletedInsideRange = (lineNumbers: number[], startLine: number, endLine: number) => {
  return lineNumbers.filter((lineNumber) => lineNumber >= startLine && lineNumber <= endLine).length
}

/**
 * 删掉行高/列宽
 * @param values 数组
 * @param deletedLineNumbers 要删除的行号/列号
 * @returns 删掉行高/列宽后的数组
 */
const omitLineValues = <T>(values: T[], deletedLineNumbers: number[]) => {
  const deletedLineSet = new Set(deletedLineNumbers)

  return values.filter((_, index) => !deletedLineSet.has(index + 1))
}

/**
 * 通过可见单元格重建表格
 * @param rows 表格行数
 * @param columns 表格列数
 * @param visibleCells 可见单元格列表
 * @param columnWidths 列宽列表
 * @param rowHeights 行高列表
 * @returns 重建后的表格数据
 */
const rebuildTableByVisibleCells = (
  rows: number,
  columns: number,
  visibleCells: EditorCanvasCell[],
  columnWidths: number[],
  rowHeights: number[],
): EditorCanvasTable => {
  // 记录哪些位置是真正的主单元格
  const masterCellMap = new Map<string, EditorCanvasCell>()
  // 记录哪些位置是合并单元格
  const mergedCellParentMap = new Map<string, string>()

  visibleCells.forEach((cell) => {
    const normalizedCell = {
      ...cell,
      merged: false,
      mergeParentId: '',
    }
    const range = getCellRange(normalizedCell)

    masterCellMap.set(createCellKey(normalizedCell.row, normalizedCell.col), normalizedCell)

    for (let row = range.rowStart; row <= range.rowEnd; row += 1) {
      for (let col = range.colStart; col <= range.colEnd; col += 1) {
        if (row === normalizedCell.row && col === normalizedCell.col) {
          continue
        }

        mergedCellParentMap.set(createCellKey(row, col), normalizedCell.id)
      }
    }
  })

  const cells: EditorCanvasCell[] = []

  for (let row = 1; row <= rows; row += 1) {
    for (let col = 1; col <= columns; col += 1) {
      const cellKey = createCellKey(row, col)
      const masterCell = masterCellMap.get(cellKey)

      if (masterCell) {
        cells.push(masterCell)
        continue
      }

      const mergeParentId = mergedCellParentMap.get(cellKey)

      if (mergeParentId) {
        cells.push({
          ...createCell(row, col),
          merged: true,
          mergeParentId,
        })
        continue
      }

      cells.push(createCell(row, col))
    }
  }

  return {
    rows,
    columns,
    cells: sortCells(cells),
    columnWidths,
    rowHeights,
  }
}

/**
 * 删除指定行
 */
export const deleteRows = (table: EditorCanvasTable, rowNumbers: number[]) => {
  // 1.整理要删除的行号
  const normalizedRows = normalizeLineNumbers(rowNumbers, table.rows)

  if (!normalizedRows.length) {
    return table
  }

  if (normalizedRows.length >= table.rows) {
    return null
  }

  // 2. 筛选出所有可见单元格
  const nextVisibleCells = getVisibleCells(table).flatMap((cell) => {
    // 2.1 计算这个单元格的范围
    const range = getCellRange(cell)
    // 2.2 计算这个单元格前面删了多少行
    const deletedBefore = countDeletedBefore(normalizedRows, range.rowStart)
    // 2.3 计算这个单元格自己覆盖的范围里，删了多少行
    const deletedInsideRange = countDeletedInsideRange(normalizedRows, range.rowStart, range.rowEnd)
    // 2.4 计算新的行跨度
    const nextRowSpan = cell.rowSpan - deletedInsideRange

    if (nextRowSpan <= 0) {
      return []
    }

    return [
      {
        ...cell,
        row: cell.row - deletedBefore,
        rowSpan: nextRowSpan,
        merged: false,
        mergeParentId: '',
      },
    ]
  })

  return rebuildTableByVisibleCells(
    table.rows - normalizedRows.length,
    table.columns,
    nextVisibleCells,
    [...table.columnWidths],
    // 3. 删掉行高
    omitLineValues(table.rowHeights, normalizedRows),
  )
}

/**
 * 删除整行
 * @param table 表格数据
 * @param cellId 单元格id
 * @returns 删除后的表格数据
 */
export const deleteRow = (table: EditorCanvasTable, cellId: string) => {
  const activeCell = getCellById(table, cellId)

  if (!activeCell) {
    return table
  }

  return deleteRows(table, [activeCell.row])
}

/**
 * 删除指定列
 */
export const deleteColumns = (table: EditorCanvasTable, columnNumbers: number[]) => {
  // 1.把输入整理干净
  const normalizedColumns = normalizeLineNumbers(columnNumbers, table.columns)

  if (!normalizedColumns.length) {
    return table
  }

  if (normalizedColumns.length >= table.columns) {
    return null
  }

  // 2.遍历所有可见单元格，计算删完后还剩什么
  const nextVisibleCells = getVisibleCells(table).flatMap((cell) => {
    const range = getCellRange(cell)
    // 2.1 这个单元格前面删了多少列
    const deletedBefore = countDeletedBefore(normalizedColumns, range.colStart)
    // 2.2 这个单元格自己覆盖的范围内删了多少列
    const deletedInsideRange = countDeletedInsideRange(normalizedColumns, range.colStart, range.colEnd)
    // 2.3 删完后还剩几列
    const nextColSpan = cell.colSpan - deletedInsideRange

    if (nextColSpan <= 0) {
      return []
    }

    return [
      {
        ...cell,
        col: cell.col - deletedBefore,
        colSpan: nextColSpan,
        merged: false,
        mergeParentId: '',
      },
    ]
  })

  return rebuildTableByVisibleCells(
    table.rows,
    table.columns - normalizedColumns.length,
    nextVisibleCells,
    // 同步删掉列宽
    omitLineValues(table.columnWidths, normalizedColumns),
    [...table.rowHeights],
  )
}

/**
 * 删除整列
 * @param table 表格数据
 * @param cellId 单元格id
 * @returns 删除后的表格数据
 */
export const deleteColumn = (table: EditorCanvasTable, cellId: string) => {
  const activeCell = getCellById(table, cellId)

  if (!activeCell) {
    return table
  }

  return deleteColumns(table, [activeCell.col])
}

/**
 * 将插入数量收口为正整数，避免异常值破坏表格结构。
 */
const normalizeInsertCount = (count: number) => {
  return Math.max(1, Math.floor(count))
}

const insertRows = (
  table: EditorCanvasTable,
  cellId: string,
  defaultRowHeight: number,
  count: number,
  direction: 'above' | 'below',
) => {
  const activeCell = getCellById(table, cellId)

  if (!activeCell) {
    return table
  }

  const safeCount = normalizeInsertCount(count)
  const insertedRow = direction === 'above' ? activeCell.row : activeCell.row + 1
  const expandableMasters = getVisibleCells(table).filter((cell) => {
    const range = getCellRange(cell)

    return range.rowStart < insertedRow && range.rowEnd >= insertedRow
  })

  const shiftedCells = table.cells.map((cell) => {
    if (cell.row >= insertedRow) {
      return {
        ...cell,
        row: cell.row + safeCount,
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
      rowSpan: cell.rowSpan + safeCount,
    }
  })

  for (let row = insertedRow; row < insertedRow + safeCount; row += 1) {
    for (let col = 1; col <= table.columns; col += 1) {
      const owner = expandableMasters.find((cell) => {
        const range = getCellRange(cell)

        return range.colStart <= col && range.colEnd >= col
      })

      nextCells.push(
        owner
          ? {
              ...createCell(row, col),
              merged: true,
              mergeParentId: owner.id,
            }
          : createCell(row, col),
      )
    }
  }

  const nextRowHeights = [...table.rowHeights]
  nextRowHeights.splice(insertedRow - 1, 0, ...Array.from({ length: safeCount }, () => defaultRowHeight))

  return {
    ...table,
    rows: table.rows + safeCount,
    rowHeights: nextRowHeights,
    cells: sortCells(nextCells),
  }
}

/**
 * 在上方插入整行
 */
export const insertRowAbove = (table: EditorCanvasTable, cellId: string, defaultRowHeight: number, count: number) => {
  return insertRows(table, cellId, defaultRowHeight, count, 'above')
}

/**
 * 在下方插入整行
 */
export const insertRowBelow = (table: EditorCanvasTable, cellId: string, defaultRowHeight: number, count: number) => {
  return insertRows(table, cellId, defaultRowHeight, count, 'below')
}

const insertColumns = (
  table: EditorCanvasTable,
  cellId: string,
  defaultColumnWidth: number,
  count: number,
  direction: 'left' | 'right',
) => {
  const activeCell = getCellById(table, cellId)

  if (!activeCell) {
    return table
  }

  const safeCount = normalizeInsertCount(count)
  const insertedColumn = direction === 'left' ? activeCell.col : activeCell.col + 1
  const expandableMasters = getVisibleCells(table).filter((cell) => {
    const range = getCellRange(cell)

    return range.colStart < insertedColumn && range.colEnd >= insertedColumn
  })

  const shiftedCells = table.cells.map((cell) => {
    if (cell.col >= insertedColumn) {
      return {
        ...cell,
        col: cell.col + safeCount,
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
      colSpan: cell.colSpan + safeCount,
    }
  })

  for (let col = insertedColumn; col < insertedColumn + safeCount; col += 1) {
    for (let row = 1; row <= table.rows; row += 1) {
      const owner = expandableMasters.find((cell) => {
        const range = getCellRange(cell)

        return range.rowStart <= row && range.rowEnd >= row
      })

      nextCells.push(
        owner
          ? {
              ...createCell(row, col),
              merged: true,
              mergeParentId: owner.id,
            }
          : createCell(row, col),
      )
    }
  }

  const nextColumnWidths = [...table.columnWidths]
  nextColumnWidths.splice(insertedColumn - 1, 0, ...Array.from({ length: safeCount }, () => defaultColumnWidth))

  return {
    ...table,
    columns: table.columns + safeCount,
    columnWidths: nextColumnWidths,
    cells: sortCells(nextCells),
  }
}

/**
 * 在左侧插入整列
 */
export const insertColumnLeft = (
  table: EditorCanvasTable,
  cellId: string,
  defaultColumnWidth: number,
  count: number,
) => {
  return insertColumns(table, cellId, defaultColumnWidth, count, 'left')
}

/**
 * 在右侧插入整列
 */
export const insertColumnRight = (
  table: EditorCanvasTable,
  cellId: string,
  defaultColumnWidth: number,
  count: number,
) => {
  return insertColumns(table, cellId, defaultColumnWidth, count, 'right')
}
