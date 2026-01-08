import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getUserInfo, logout } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  async function loginAction(username, password) {
    const res = await login({ username, password })
    if (res.code === 200) {
      token.value = res.data.token
      userInfo.value = {
        userId: res.data.userId,
        username: res.data.username,
        nickname: res.data.nickname,
        avatar: res.data.avatar
      }
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
    return res
  }

  async function registerAction(userData) {
    return await register(userData)
  }

  async function logoutAction() {
    try {
      await logout()
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }

  async function fetchUserInfo() {
    if (!token.value) return
    const res = await getUserInfo()
    if (res.code === 200) {
      userInfo.value = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    loginAction,
    registerAction,
    logoutAction,
    fetchUserInfo
  }
})
