import request from './request'

// 获取会话列表
export function getConversationList() {
  return request.get('/message/conversations')
}

// 获取或创建与目标用户的会话
export function getOrCreateConversation(targetUserId) {
  return request.get(`/message/conversation/with/${targetUserId}`)
}

// 获取会话消息列表
export function getMessageList(conversationId, page = 1, size = 20) {
  return request.get(`/message/conversation/${conversationId}/messages`, {
    params: { page, size }
  })
}

// 发送消息
export function sendMessage(data) {
  return request.post('/message/send', data)
}

// 标记会话已读
export function markConversationRead(conversationId) {
  return request.post(`/message/conversation/${conversationId}/read`)
}

// 删除会话
export function deleteConversation(conversationId) {
  return request.delete(`/message/conversation/${conversationId}`)
}

// 获取未读消息总数
export function getUnreadCount() {
  return request.get('/message/unread/count')
}
