import { axios } from '@/utils/request'

export function validateTransformMeta (data) {
  return axios({
    url: '/api/job/validate_transform_meta',
    method: 'POST',
    data: data
  })
}

