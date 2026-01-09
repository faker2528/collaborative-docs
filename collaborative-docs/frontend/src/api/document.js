import request from './request'

export function getDocumentList() {
  return request.get('/document/list')
}

export function getDocument(id) {
  return request.get(`/document/${id}`)
}

export function createDocument(data) {
  return request.post('/document', data)
}

export function updateDocument(id, content) {
  return request.put(`/document/${id}`, content, {
    headers: { 'Content-Type': 'text/plain' }
  })
}

export function deleteDocument(id) {
  return request.delete(`/document/${id}`)
}

export function shareDocument(id, targetUserId, permissionType) {
  return request.post(`/document/${id}/share`, null, {
    params: { targetUserId, permissionType }
  })
}

// 分享链接相关
export function createShareLink(data) {
  return request.post('/share/link', data)
}

export function joinByShareLink(token) {
  return request.post(`/share/join/${token}`)
}

export function getShareLinks(documentId) {
  return request.get(`/share/links/${documentId}`)
}

export function disableShareLink(linkId) {
  return request.delete(`/share/link/${linkId}`)
}

export function getShareLinkInfo(token) {
  return request.get(`/share/info/${token}`)
}

// 文档成员管理
export function getDocumentMembers(documentId) {
  return request.get(`/document/${documentId}/members`)
}

export function updateMemberPermission(documentId, targetUserId, permissionType) {
  return request.put(`/document/${documentId}/members/${targetUserId}`, null, {
    params: { permissionType }
  })
}

export function removeMember(documentId, targetUserId) {
  return request.delete(`/document/${documentId}/members/${targetUserId}`)
}
