import type { FormEntityId } from '@/api/form-group/type'
import type { EditorSchema } from '@/types/editor'

export type FormVersionPersistReq = {
  schemaJson: EditorSchema
}

export type FormVersionPersistResult = {
  currentVersionId: FormEntityId | null
  publishedVersionId: FormEntityId | null
  versionNo: number
  alreadyPublished: boolean
}
