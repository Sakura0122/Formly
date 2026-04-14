import request from '@/utils/request'

import type { FormEntityId } from '@/api/form-group/type'
import type { FormDefinitionCreateReq, FormDefinitionDetail, FormDefinitionUpdateReq } from './type'

export const formDefinitionApi = {
  getDetail(id: FormEntityId) {
    return request.get<FormDefinitionDetail>(`/form-definitions/${id}`).then((res) => res.data)
  },
  create(data: FormDefinitionCreateReq) {
    return request.post<FormEntityId>('/form-definitions', data).then((res) => res.data)
  },
  update(id: FormEntityId, data: FormDefinitionUpdateReq) {
    return request.put<null>(`/form-definitions/${id}`, data).then((res) => res.data)
  },
  delete(id: FormEntityId) {
    return request.delete<null>(`/form-definitions/${id}`).then((res) => res.data)
  },
}
