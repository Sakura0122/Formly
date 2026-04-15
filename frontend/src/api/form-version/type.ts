import type { FormEntityId } from '@/api/form-group/type'
import type { EditorSchema } from '@/types/editor'

export type FormVersionPersistReq = {
  schemaJson: EditorSchema
}

export type FormVersionPersistResult = {
  publishedVersionId: FormEntityId | null
  hasUnpublishedDraft: boolean
  versionNo: number | null
  alreadyPublished: boolean
}
