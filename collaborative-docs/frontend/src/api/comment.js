import request from './request'

// 获取评论列表
export function getComments(documentId) {
  return request.get(`/comment/${documentId}/list`)
}

// 添加评论
export function addComment(data) {
  return request.post('/comment', data)
}

// 删除评论
export function deleteComment(commentId) {
  return request.delete(`/comment/${commentId}`)
}

// 回复评论
export function replyComment(data) {
  return request.post('/comment/reply', data)
}

// 点赞评论
export function likeComment(commentId) {
  return request.post(`/comment/${commentId}/like`)
}
