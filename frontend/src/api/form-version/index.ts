import request from '@/utils/request'

import type { FormEntityId } from '@/api/form-group/type'
import type { FormVersionPersistReq, FormVersionPersistResult } from './type'

export const formVersionApi = {
  async save(formId: FormEntityId, data: FormVersionPersistReq) {
    const res = await request.post<FormVersionPersistResult>(`/form-definitions/${formId}/save`, data)
    return res.data
  },
  async publish(formId: FormEntityId, data: FormVersionPersistReq) {
    const res = await request.post<FormVersionPersistResult>(`/form-definitions/${formId}/publish`, data)
    return res.data
  },
}
