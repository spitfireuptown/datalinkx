import { axios } from '@/utils/request'

export function listQuery () {
  return axios({
    url: '/api/alarm/component/list',
    method: 'GET'
  })
}
export function addObj (obj) {
  return axios({
    url: '/api/alarm/component/create',
    method: 'POST',
    data: obj
  })
}
export function putObj (obj) {
  return axios({
    url: '/api/alarm/component/modify',
    method: 'POST',
    data: obj
  })
}
export function getObj (id) {
  return axios({
    url: `/api/alarm/component/info/${id}`,
    method: 'get'
  })
}
export function delObj (id) {
  return axios({
    url: `/api/alarm/component/delete/${id}`,
    method: 'POST'
  })
}
export function listRuleQuery () {
  return axios({
    url: '/api/alarm/rule/list',
    method: 'GET'
  })
}
export function shutdownObj (id) {
  return axios({
    url: `/api/alarm/rule/shutdown/${id}`,
    method: 'POST'
  })
}
export function delRuleObj (id) {
  return axios({
    url: `/api/alarm/rule/delete/${id}`,
    method: 'POST'
  })
}

export function getInSiteMessageList (userId, page = 1, size = 10) {
  return axios({
    url: `/api/alarm/in-site-message/list/${userId}`,
    method: 'GET',
    params: {
      page,
      size
    }
  })
}

export function markInSiteMessageAsRead (userId, messageId = null) {
  return axios({
    url: `/api/alarm/in-site-message/read/${userId}`,
    method: 'PUT',
    params: {
      messageId
    }
  })
}

export function markAllInSiteMessagesAsRead (userId) {
  return axios({
    url: `/api/alarm/in-site-message/read-all/${userId}`,
    method: 'PUT'
  })
}
