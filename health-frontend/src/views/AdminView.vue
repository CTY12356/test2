<script setup>
import { onMounted, ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUsers, banUser, unbanUser,
  getAllPosts, auditPost, deletePost,
  getCommentsByPost, deleteComment,
  getReports, handleReport,
} from '../api/admin'

const activeTab = ref('users')

// ---- Users ----
const users = ref([])
const loadingUsers = ref(false)

async function loadUsers() {
  if (loadingUsers.value) return
  loadingUsers.value = true
  try {
    const { data } = await getUsers()
    users.value = data.success ? (data.data || []) : []
  } catch {
    ElMessage.error('加载用户列表失败')
  } finally {
    loadingUsers.value = false
  }
}

async function toggleBan(user) {
  const isBanned = user.status === 'BANNED'
  const label = isBanned ? '解封' : '封禁'
  try {
    await ElMessageBox.confirm(`确定要${label}用户 ${user.username} 吗？`, '确认', { type: 'warning' })
  } catch { return }
  try {
    const fn = isBanned ? unbanUser : banUser
    const { data } = await fn(user.id)
    if (data.success) {
      const idx = users.value.findIndex(u => u.id === user.id)
      if (idx !== -1) users.value[idx] = data.data
      ElMessage.success(`${label}成功`)
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

// ---- Posts ----
const posts = ref([])
const loadingPosts = ref(false)
const expandedPostId = ref(null)
const commentsMap = ref({})
const loadingComments = ref({})

async function loadPosts() {
  if (loadingPosts.value) return
  loadingPosts.value = true
  try {
    const { data } = await getAllPosts()
    posts.value = data.success ? (data.data || []) : []
  } catch {
    ElMessage.error('加载帖子失败')
  } finally {
    loadingPosts.value = false
  }
}

async function togglePostAudit(post) {
  const toStatus = post.auditStatus === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED'
  const label = toStatus === 'HIDDEN' ? '隐藏' : '恢复'
  try {
    const { data } = await auditPost(post.id, toStatus)
    if (data.success) {
      const idx = posts.value.findIndex(p => p.id === post.id)
      if (idx !== -1) posts.value[idx] = { ...posts.value[idx], auditStatus: toStatus }
      ElMessage.success(`帖子已${label}`)
    } else {
      ElMessage.error(data.message || '操作失败')
    }
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '操作失败'
    ElMessage.error(`${label}失败：${msg}`)
  }
}

async function handleDeletePost(post) {
  try {
    await ElMessageBox.confirm(
      `确定要删除帖子《${post.title}》吗？该操作不可恢复，帖子下所有评论也将一并删除。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch { return }
  try {
    const { data } = await deletePost(post.id)
    if (data.success) {
      posts.value = posts.value.filter(p => p.id !== post.id)
      if (expandedPostId.value === post.id) expandedPostId.value = null
      ElMessage.success('帖子已删除')
    } else {
      ElMessage.error(data.message || '删除失败')
    }
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '删除失败'
    ElMessage.error(`删除失败：${msg}`)
  }
}

async function toggleComments(post) {
  if (expandedPostId.value === post.id) {
    expandedPostId.value = null
    return
  }
  expandedPostId.value = post.id
  if (commentsMap.value[post.id] !== undefined) return
  loadingComments.value = { ...loadingComments.value, [post.id]: true }
  try {
    const { data } = await getCommentsByPost(post.id)
    commentsMap.value = {
      ...commentsMap.value,
      [post.id]: data.success ? (data.data || []) : [],
    }
  } catch {
    ElMessage.error('加载评论失败')
    commentsMap.value = { ...commentsMap.value, [post.id]: [] }
  } finally {
    loadingComments.value = { ...loadingComments.value, [post.id]: false }
  }
}

async function handleDeleteComment(postId, comment) {
  try {
    await ElMessageBox.confirm('确定要删除该评论吗？', '删除确认', { type: 'warning' })
  } catch { return }
  try {
    const { data } = await deleteComment(comment.id)
    if (data.success) {
      commentsMap.value = {
        ...commentsMap.value,
        [postId]: (commentsMap.value[postId] || []).filter(c => c.id !== comment.id),
      }
      const post = posts.value.find(p => p.id === postId)
      if (post) post.commentCount = Math.max(0, (post.commentCount || 1) - 1)
      ElMessage.success('评论已删除')
    }
  } catch {
    ElMessage.error('删除失败')
  }
}

// Computed helpers for expanded comments
const expandedPost = computed(() => posts.value.find(p => p.id === expandedPostId.value))
const expandedComments = computed(() => expandedPostId.value !== null ? (commentsMap.value[expandedPostId.value] || []) : [])
const isLoadingExpandedComments = computed(() => expandedPostId.value !== null && !!loadingComments.value[expandedPostId.value])

// ---- Reports ----
const reports = ref([])
const loadingReports = ref(false)
const handleDialogVisible = ref(false)
const currentReport = ref(null)
const handleAction = ref('IGNORE')
const handleResult = ref('')

const actionOptions = [
  { value: 'IGNORE', label: '不做操作', desc: '仅标记举报已处理，内容保留不变' },
  { value: 'HIDE', label: '隐藏内容', desc: '将被举报的帖子/评论隐藏，对普通用户不可见' },
  { value: 'DELETE', label: '删除内容', desc: '永久删除被举报的帖子/评论，不可恢复' },
]

async function loadReports() {
  if (loadingReports.value) return
  loadingReports.value = true
  try {
    const { data } = await getReports()
    reports.value = data.success ? (data.data || []) : []
  } catch {
    ElMessage.error('加载举报失败')
  } finally {
    loadingReports.value = false
  }
}

function openHandleDialog(report) {
  currentReport.value = report
  handleAction.value = 'IGNORE'
  handleResult.value = ''
  handleDialogVisible.value = true
}

async function submitHandle() {
  try {
    const { data } = await handleReport(currentReport.value.id, handleAction.value, handleResult.value)
    if (data.success) {
      const idx = reports.value.findIndex(r => r.id === currentReport.value.id)
      if (idx !== -1) reports.value[idx] = data.data
      // 如果选择了删除，同步更新帖子列表
      if (handleAction.value === 'DELETE' && currentReport.value.targetType === 'POST') {
        posts.value = posts.value.filter(p => p.id !== currentReport.value.targetId)
      }
      if (handleAction.value === 'HIDE' && currentReport.value.targetType === 'POST') {
        const postIdx = posts.value.findIndex(p => p.id === currentReport.value.targetId)
        if (postIdx !== -1) posts.value[postIdx] = { ...posts.value[postIdx], auditStatus: 'HIDDEN' }
      }
      ElMessage.success('举报已处理，系统已通知举报用户')
      handleDialogVisible.value = false
    } else {
      ElMessage.error(data.message || '处理失败')
    }
  } catch (err) {
    const msg = err.response?.data?.message || err.message || '处理失败'
    ElMessage.error(`处理失败：${msg}`)
  }
}

// Watch activeTab to load data on demand
watch(activeTab, (tab) => {
  if (tab === 'posts') loadPosts()
  if (tab === 'reports') loadReports()
})

// Preload all data on mount
onMounted(() => {
  loadUsers()
  loadPosts()
  loadReports()
})
</script>

<template>
  <h2 class="page-title">后台管理</h2>

  <el-tabs v-model="activeTab">
    <!-- 用户管理 -->
    <el-tab-pane label="用户管理" name="users">
      <el-table :data="users" v-loading="loadingUsers" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '已封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              v-if="row.role !== 'ADMIN'"
              :type="row.status === 'BANNED' ? 'success' : 'danger'"
              size="small"
              link
              @click="toggleBan(row)"
            >
              {{ row.status === 'BANNED' ? '解封' : '封禁' }}
            </el-button>
            <span v-else style="font-size:12px; color:#9ca3af;">—</span>
          </template>
        </el-table-column>
      </el-table>
    </el-tab-pane>

    <!-- 帖子管理 -->
    <el-tab-pane label="帖子管理" name="posts">
      <el-table :data="posts" v-loading="loadingPosts" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="authorNickname" label="作者" width="100" />
        <el-table-column prop="category" label="分类" width="110" show-overflow-tooltip />
        <el-table-column label="评论" width="65" align="center">
          <template #default="{ row }">{{ row.commentCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.auditStatus === 'PUBLISHED' ? 'success' : 'warning'" size="small">
              {{ row.auditStatus === 'PUBLISHED' ? '已发布' : '已隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="165">
          <template #default="{ row }">
            <el-button
              :type="row.auditStatus === 'PUBLISHED' ? 'warning' : 'success'"
              size="small"
              link
              @click="togglePostAudit(row)"
            >{{ row.auditStatus === 'PUBLISHED' ? '隐藏' : '恢复' }}</el-button>
            <el-button size="small" link @click="toggleComments(row)">
              {{ expandedPostId === row.id ? '收起' : '评论' }}
            </el-button>
            <el-button type="danger" size="small" link @click="handleDeletePost(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 展开的评论列表 -->
      <el-card v-if="expandedPostId !== null" style="margin-top:12px; background:#f8fafc;" shadow="never">
        <template #header>
          <span style="font-weight:600; font-size:14px;">
            《{{ expandedPost?.title }}》的评论
          </span>
        </template>
        <div v-if="isLoadingExpandedComments" v-loading="true" style="height:60px;" />
        <el-empty v-else-if="!expandedComments.length" description="暂无评论" :image-size="50" />
        <div v-else>
          <div
            v-for="c in expandedComments"
            :key="c.id"
            style="display:flex; align-items:flex-start; gap:10px; padding:8px 0; border-bottom:1px solid #e5e7eb;"
          >
            <el-avatar :size="28" style="background:#10b981; font-size:12px; flex-shrink:0;">
              {{ (c.authorNickname || '?')[0].toUpperCase() }}
            </el-avatar>
            <div style="flex:1; min-width:0;">
              <span style="font-weight:600; font-size:13px; margin-right:8px;">{{ c.authorNickname }}</span>
              <span style="font-size:11px; color:#9ca3af;">{{ c.createdAt }}</span>
              <div style="font-size:13px; color:#374151; margin-top:2px; word-break:break-all;">{{ c.content }}</div>
            </div>
            <el-button
              type="danger"
              size="small"
              link
              style="flex-shrink:0;"
              @click="handleDeleteComment(expandedPostId, c)"
            >删除</el-button>
          </div>
        </div>
      </el-card>
    </el-tab-pane>

    <!-- 举报处理 -->
    <el-tab-pane label="举报处理" name="reports">
      <el-table :data="reports" v-loading="loadingReports" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="targetType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.targetType === 'POST' ? '帖子' : '评论' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetId" label="内容ID" width="80" />
        <el-table-column prop="reason" label="举报原因" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : 'info'" size="small">
              {{ row.status === 'PENDING' ? '待处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="处理结果" show-overflow-tooltip />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary"
              size="small"
              link
              @click="openHandleDialog(row)"
            >处理</el-button>
            <span v-else style="font-size:12px; color:#9ca3af;">已完成</span>
          </template>
        </el-table-column>
      </el-table>
    </el-tab-pane>
  </el-tabs>

  <!-- 处理举报弹窗 -->
  <el-dialog v-model="handleDialogVisible" title="处理举报" width="480px" :close-on-click-modal="false">
    <div style="margin-bottom:16px; padding:12px; background:#f8fafc; border-radius:8px; font-size:13px; color:#374151;">
      <div><span style="color:#9ca3af;">举报类型：</span>{{ currentReport?.targetType === 'POST' ? '帖子' : '评论' }}（ID: {{ currentReport?.targetId }}）</div>
      <div style="margin-top:4px;"><span style="color:#9ca3af;">举报原因：</span><strong>{{ currentReport?.reason }}</strong></div>
    </div>

    <div style="margin-bottom:16px;">
      <div style="font-size:13px; font-weight:600; color:#374151; margin-bottom:8px;">选择处理方式</div>
      <div style="display:flex; flex-direction:column; gap:8px;">
        <div
          v-for="opt in actionOptions"
          :key="opt.value"
          @click="handleAction = opt.value"
          style="padding:10px 14px; border-radius:8px; cursor:pointer; border:2px solid transparent; transition:all 0.15s;"
          :style="{
            borderColor: handleAction === opt.value ? '#409eff' : '#e5e7eb',
            background: handleAction === opt.value ? '#eff6ff' : '#fff',
          }"
        >
          <div style="display:flex; align-items:center; gap:8px;">
            <el-radio :model-value="handleAction" :label="opt.value" @change="handleAction = opt.value" />
            <span style="font-weight:600; font-size:13px;">{{ opt.label }}</span>
          </div>
          <div style="font-size:12px; color:#6b7280; margin-top:2px; margin-left:24px;">{{ opt.desc }}</div>
        </div>
      </div>
    </div>

    <div>
      <div style="font-size:13px; font-weight:600; color:#374151; margin-bottom:6px;">
        向举报者的说明（可选）
      </div>
      <el-input
        v-model="handleResult"
        type="textarea"
        :rows="3"
        placeholder="如：已核实内容违规，已按规定处理；内容经审核符合社区规范，举报不成立等"
      />
      <div style="font-size:11px; color:#9ca3af; margin-top:4px;">填写后举报者将收到系统通知</div>
    </div>

    <template #footer>
      <el-button @click="handleDialogVisible = false">取消</el-button>
      <el-button
        :type="handleAction === 'DELETE' ? 'danger' : 'primary'"
        @click="submitHandle"
      >确认处理</el-button>
    </template>
  </el-dialog>
</template>
