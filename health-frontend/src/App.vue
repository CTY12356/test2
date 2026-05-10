<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from './stores/user'
import { getUnreadCount } from './api/notification'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const unreadCount = ref(0)
let pollTimer = null

async function fetchUnread() {
  if (!userStore.isLoggedIn) return
  try {
    const { data } = await getUnreadCount()
    if (data.success) unreadCount.value = data.data.count
  } catch { /* silent */ }
}

function startPolling() {
  fetchUnread()
  pollTimer = setInterval(fetchUnread, 30000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

watch(() => userStore.isLoggedIn, (v) => {
  if (v) startPolling()
  else { stopPolling(); unreadCount.value = 0 }
}, { immediate: true })

// 访问通知页时重新拉取未读数
watch(() => route.path, (path) => {
  if (path === '/notifications') {
    setTimeout(fetchUnread, 500)
  }
})

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.syncFromServer()
  }
})

onUnmounted(stopPolling)

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<template>
  <div v-if="route.path === '/login' || route.path === '/register'">
    <router-view />
  </div>

  <el-container v-else class="app-shell">
    <el-aside width="220px" class="sidebar">
      <div class="sidebar-logo">
        <span class="logo-icon">💪</span>
        <span class="logo-text">健康管理</span>
      </div>
      <el-menu :router="true" :default-active="route.path" class="sidebar-menu">
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>数据总览</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><User /></el-icon>
          <span>健康档案</span>
        </el-menu-item>
        <el-menu-item index="/checkin">
          <el-icon><Edit /></el-icon>
          <span>每日打卡</span>
        </el-menu-item>
        <el-menu-item index="/forum">
          <el-icon><ChatDotRound /></el-icon>
          <span>用户论坛</span>
        </el-menu-item>
        <el-menu-item index="/user-center">
          <el-icon><Setting /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
        <el-menu-item index="/notifications">
          <el-icon><Bell /></el-icon>
          <span>
            消息通知
            <el-badge v-if="unreadCount > 0" :value="unreadCount > 99 ? '99+' : unreadCount"
              style="margin-left:4px;" />
          </span>
        </el-menu-item>
        <el-menu-item v-if="userStore.role === 'ADMIN'" index="/admin">
          <el-icon><Tools /></el-icon>
          <span>后台管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <span class="disclaimer">健康数据仅供参考，不能替代医生或营养师建议。</span>
        <div v-if="userStore.username" class="topbar-user">
          <el-badge :value="unreadCount > 0 ? (unreadCount > 99 ? '99+' : unreadCount) : ''"
            :hidden="unreadCount === 0"
            style="cursor:pointer; margin-right:4px;"
            @click="router.push('/notifications')"
          >
            <el-icon style="font-size:20px; color:#6b7280; vertical-align:middle;"><Bell /></el-icon>
          </el-badge>
          <el-avatar
            :size="32"
            :src="userStore.userInfo?.avatarUrl || undefined"
            style="background:#409eff; font-size:13px; flex-shrink:0; cursor:pointer;"
            @click="router.push('/user-center')"
          >
            {{ (userStore.userInfo?.nickname || userStore.username)[0].toUpperCase() }}
          </el-avatar>
          <span>{{ userStore.userInfo?.nickname || userStore.username }}</span>
          <el-button type="danger" link size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style>
/* Import element icons at app level */
</style>
