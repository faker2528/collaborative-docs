import request from './request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function logout() {
  return request.post('/user/logout')
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function getUserById(userId) {
  return request.get(`/user/${userId}`)
}

export function searchUsers(keyword) {
  return request.get('/user/search', { params: { keyword } })
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}
