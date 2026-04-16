import type { FormEntityId } from '@/api/form-group/type'
import type { EditorSchema } from '@/types/editor'

export type FormDefinitionCreateReq = {
  groupId: FormEntityId | null
  name: string
  formKey: string
  description?: string
  sort?: number
}

export type FormDefinitionUpdateReq = {
  groupId?: FormEntityId | null
  name: string
  description?: string
  sort?: number
}

export type FormDefinitionEditorDetail = {
  id: FormEntityId
  groupId: FormEntityId | null
  name: string
  formKey: string
  description: string
  publishedVersionId: FormEntityId | null
  hasUnpublishedDraft: boolean
  sort: number | null
  schema: EditorSchema | null
  createdAt: string
  updatedAt: string
}

export type FormDefinitionHistoryItem = {
  id: FormEntityId
  versionNo: number
  schema: EditorSchema
  createdAt: string
  createdBy: string
}

export type FormDefinitionFormDetail = {
  id: FormEntityId
  name: string
  formKey: string
  publishedVersionNo: number | null
  schema: EditorSchema | null
  updatedAt: string
}

export type FormDefinitionPasteParseReq = {
  documentHtml?: string
  plainText?: string
}

export type FormDefinitionPasteParseResult = {
  schemaJson: EditorSchema
}
