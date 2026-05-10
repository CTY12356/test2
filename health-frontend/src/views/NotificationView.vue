<script setup>
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotifications, markAllRead, markRead } from '../api/notification'

const notifications = ref([])
const loading = ref(false)

const typeMap = {
  LIKE: { label: '点赞', icon: '👍', color: '#f59e0b' },
  COMMENT: { label: '评论', icon: '💬', color: '#3b82f6' },
  REPORT_HANDLED: { label: '举报反馈', icon: '🛡️', color: '#8b5cf6' },
}

const hasUnread = computed(() => notifications.value.some(n => !n.isRead))

async function load() {
  loading.value = true
  try {
    const { data } = await getNotifications()
    notifications.value = data.success ? (data.data || []) : []
  } catch {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

async function handleMarkAllRead() {
  try {
    await markAllRead()
    notifications.value.forEach(n => (n.isRead = true))
    ElMessage.success('已全部标记为已读')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleMarkRead(n) {
  if (n.isRead) return
  try {
    await markRead(n.id)
    n.isRead = true
  } catch { /* silent */ }
}

function formatTime(str) {
  if (!str) return ''
  const d = new Date(str)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  return d.toLocaleDateString('zh-CN')
}

onMounted(load)
</script>

<template>
  <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:16px;">
    <h2 class="page-title" style="margin:0;">消息通知</h2>
    <el-button v-if="hasUnread" size="small" @click="handleMarkAllRead">全部标为已读</el-button>
  </div>

  <div v-loading="loading" style="min-height:200px;">
    <el-empty v-if="!loading && !notifications.length" description="暂无通知" :image-size="80" />

    <div v-else style="display:flex; flex-direction:column; gap:8px;">
      <div
        v-for="n in notifications"
        :key="n.id"
        @click="handleMarkRead(n)"
        style="background:#fff; border-radius:10px; padding:14px 18px; display:flex;
               align-items:flex-start; gap:14px; cursor:pointer;
               box-shadow:0 1px 4px rgba(0,0,0,0.06); border-left:4px solid transparent;
               transition:background 0.15s;"
        :style="{
          borderLeftColor: n.isRead ? '#e5e7eb' : (typeMap[n.type]?.color || '#409eff'),
          background: n.isRead ? '#fafafa' : '#fff',
        }"
      >
        <span style="font-size:22px; line-height:1; flex-shrink:0; margin-top:1px;">
          {{ typeMap[n.type]?.icon || '🔔' }}
        </span>
        <div style="flex:1; min-width:0;">
          <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
            <el-tag
              size="small"
              style="border:none; font-size:11px;"
              :style="{ background: typeMap[n.type]?.color || '#409eff', color: '#fff' }"
            >{{ typeMap[n.type]?.label || n.type }}</el-tag>
            <span style="font-weight:600; font-size:14px; color:#1f2937;">{{ n.title }}</span>
            <span
              v-if="!n.isRead"
              style="width:8px; height:8px; background:#ef4444; border-radius:50%; display:inline-block; flex-shrink:0;"
            />
          </div>
          <div v-if="n.content" style="margin-top:4px; font-size:13px; color:#6b7280; word-break:break-all;">
            {{ n.content }}
          </div>
          <div style="margin-top:4px; font-size:11px; color:#9ca3af;">{{ formatTime(n.createdAt) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>
