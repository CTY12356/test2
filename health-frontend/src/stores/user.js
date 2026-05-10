import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMe } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const role = computed(() => userInfo.value?.role || '')

  function setUser(data) {
    token.value = data.token
    userInfo.value = data
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
  }

  /** 用服务端用户表刷新角色等信息（JWT 鉴权后以后台数据为准） */
  async function syncFromServer() {
    if (!token.value) return
    try {
      const { data } = await getMe()
      if (!data.success || !data.data) return
      const u = data.data
      userInfo.value = {
        ...userInfo.value,
        token: token.value,
        userId: u.id,
        username: u.username,
        nickname: u.nickname,
        avatarUrl: u.avatarUrl,
        role: u.role,
        status: u.status,
      }
      localStorage.setItem('user', JSON.stringify(userInfo.value))
    } catch {
      /* 忽略：未登录或 token 失效由拦截器处理 */
    }
  }

  function updateProfile(data) {
    if (!userInfo.value) return
    userInfo.value = { ...userInfo.value, nickname: data.nickname, avatarUrl: data.avatarUrl }
    localStorage.setItem('user', JSON.stringify(userInfo.value))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, userInfo, isLoggedIn, username, role, setUser, syncFromServer, updateProfile, logout }
})
