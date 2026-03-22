export type EditorHeaderActionKey = 'import-schema' | 'export-schema' | 'shortcut'

export type EditorPaletteGroupKey = 'basic' | 'choice' | 'advanced'

export type EditorComponentType =
  | 'text'
  | 'image'
  | 'input'
  | 'textarea'
  | 'number'
  | 'radio'
  | 'checkbox'
  | 'select'
  | 'date'
  | 'switch'
  | 'upload'

export interface EditorPaletteGroup {
  key: EditorPaletteGroupKey
  label: string
  description: string
}

export interface EditorPaletteItem {
  type: EditorComponentType
  label: string
  description: string
  icon: string
  group: EditorPaletteGroupKey
  disabled?: boolean
}

export interface EditorHeaderActionOption {
  key: EditorHeaderActionKey
  label: string
  icon: string
}

export interface EditorFieldOption {
  label: string
  value: string
}

export type EditorHorizontalAlign = 'left' | 'center' | 'right'

export interface EditorFieldInstance {
  uuid: string
  type: EditorComponentType
  placeholder: string
  required: boolean
  helpText: string
  horizontalAlign: EditorHorizontalAlign
  textContent: string
  imageUrl: string
  options: EditorFieldOption[]
  switchActiveText: string
  switchInactiveText: string
}

export interface EditorCanvasCell {
  id: string
  row: number
  col: number
  fields: EditorFieldInstance[]
  rowSpan: number
  colSpan: number
  merged: boolean
  mergeParentId: string
}

export interface EditorCanvasTable {
  rows: number
  columns: number
  cells: EditorCanvasCell[]
  columnWidths: number[]
  rowHeights: number[]
}

export interface EditorCanvasDropPayload {
  cellId: string
  type: EditorComponentType
}

export interface EditorSelectFieldPayload {
  cellId: string
  fieldId: string
}

export interface EditorRemoveFieldPayload {
  cellId: string
  fieldId: string
}

export interface EditorCanvasSelectionPayload {
  activeCellId: string
  selectedCellIds: string[]
  selectionAnchorCellId: string
}

export interface EditorCanvasContextMenuPayload {
  x: number
  y: number
  cellId: string
}

export interface EditorResizeColumnPayload {
  index: number
  width: number
}

export interface EditorResizeRowPayload {
  index: number
  height: number
}

export interface EditorCreateTableForm {
  rows: number
  columns: number
}

export type EditorFieldPatch = Partial<Omit<EditorFieldInstance, 'uuid'>>

export type EditorContextMenuCommand =
  | 'create'
  | 'rebuild'
  | 'merge-cells'
  | 'split-cell'
  | 'insert-row-below'
  | 'insert-column-right'

export interface EditorContextMenuItem {
  command: EditorContextMenuCommand
  label: string
  icon: string
  disabled?: boolean
}
