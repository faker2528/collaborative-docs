import request from './request'

// 获取好友列表
export function getFriendList() {
  return request.get('/friend/list')
}

// 发送好友请求
export function sendFriendRequest(toUserId, message = '') {
  return request.post('/friend/request', { toUserId, message })
}

// 获取收到的好友请求
export function getReceivedRequests() {
  return request.get('/friend/requests/received')
}

// 获取发送的好友请求
export function getSentRequests() {
  return request.get('/friend/requests/sent')
}

// 获取待处理的好友请求数量
export function getPendingRequestCount() {
  return request.get('/friend/requests/pending/count')
}

// 获取待处理的好友请求列表（收到的未处理请求）
export function getPendingRequests() {
  return request.get('/friend/requests/received')
}

// 同意好友请求
export function acceptFriendRequest(requestId) {
  return request.post(`/friend/request/${requestId}/accept`)
}

// 拒绝好友请求
export function rejectFriendRequest(requestId) {
  return request.post(`/friend/request/${requestId}/reject`)
}

// 删除好友
export function deleteFriend(friendId) {
  return request.delete(`/friend/${friendId}`)
}

// 检查是否为好友
export function checkFriend(targetUserId) {
  return request.get(`/friend/check/${targetUserId}`)
}
