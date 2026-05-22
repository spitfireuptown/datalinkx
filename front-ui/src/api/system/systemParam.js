import request from '@/utils/request'

export function pageQuery(params) {
  return request({
    url: '/api/system/param/page',
    method: 'get',
    params
  })
}

export function list() {
  return request({
    url: '/api/system/param/list',
    method: 'get'
  })
}

export function listByScope(scope) {
  return request({
    url: '/api/system/param/list/scope',
    method: 'get',
    params: { scope }
  })
}

export function info(id) {
  return request({
    url: `/api/system/param/info/${id}`,
    method: 'get'
  })
}

export function create(data) {
  return request({
    url: '/api/system/param/create',
    method: 'post',
    data
  })
}

export function modify(data) {
  return request({
    url: '/api/system/param/modify',
    method: 'post',
    data
  })
}

export function del(id) {
  return request({
    url: `/api/system/param/delete/${id}`,
    method: 'post'
  })
}

export function toggleEnable(id) {
  return request({
    url: `/api/system/param/toggle/${id}`,
    method: 'post'
  })
}