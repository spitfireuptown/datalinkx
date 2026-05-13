import { axios } from '@/utils/request'

export function validateDynamicCode (data) {
  return axios({
    url: '/api/job/validate_dynamic_code',
    method: 'POST',
    data: data
  })
}
