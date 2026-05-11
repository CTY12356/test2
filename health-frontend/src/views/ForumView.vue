<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, ChatDotRound, Pointer, Warning } from '@element-plus/icons-vue'
import { getPosts, createPost, getComments, createComment, likePost, reportContent } from '../api/forum'
import { FORUM_CATEGORIES } from '../constants/options'
import { API_BASE } from '../constants/apiBase'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const uploadHeaders = { Authorization: `Bearer ${userStore.token}` }

const posts = ref([])
const loadingPosts = ref(false)
const publishing = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)

const filterCategory = ref('')
const filterKeyword = ref('')

const expandedComments = ref({})
const commentInputs = ref({})
const submittingComment = ref({})

const reportDialogVisible = ref(false)
const reportTarget = ref(null)
const reportReason = ref('')
const submittingReport = ref(false)

const form = reactive({
  title: '', content: '', category: '减肥打卡', tags: '', imageUrl: '',
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择帖子类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

async function loadPosts() {
  loadingPosts.value = true
  try {
    const { data } = await getPosts({ category: filterCategory.value, keyword: filterKeyword.value })
    posts.value = data.success ? (data.data || []) : []
    if (!data.success) ElMessage.error(data.message || '获取帖子失败')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '论坛加载失败，请稍后重试')
  } finally {
    loadingPosts.value = false
  }
}

function openDialog() {
  Object.assign(form, { title: '', content: '', category: '减肥打卡', tags: '', imageUrl: '' })
  dialogVisible.value = true
}

async function publish() {
  await formRef.value.validate()
  publishing.value = true
  try {
    const { data } = await createPost(form)
    if (data.success) {
      ElMessage.success('发布成功')
      dialogVisible.value = false
      await loadPosts()
    } else {
      ElMessage.error(data.message || '发布失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

function handleUpload(response) {
  if (response.success && response.data?.url) {
    form.imageUrl = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

async function handleLike(post) {
  try {
    await likePost(post.id)
    if (post.likedByCurrentUser) {
      post.likeCount = Math.max(0, post.likeCount - 1)
      post.likedByCurrentUser = false
    } else {
      post.likeCount += 1
      post.likedByCurrentUser = true
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

async function toggleComments(post) {
  const id = post.id
  if (expandedComments.value[id]) {
    expandedComments.value[id] = null
    return
  }
  try {
    const { data } = await getComments(id)
    expandedComments.value[id] = data.success ? (data.data || []) : []
    if (!commentInputs.value[id]) commentInputs.value[id] = ''
  } catch {
    ElMessage.error('加载评论失败')
  }
}

async function submitComment(post) {
  const id = post.id
  const content = (commentInputs.value[id] || '').trim()
  if (!content) { ElMessage.warning('请输入评论内容'); return }
  submittingComment.value[id] = true
  try {
    const { data } = await createComment({ postId: id, content })
    if (data.success) {
      commentInputs.value[id] = ''
      post.commentCount = (post.commentCount || 0) + 1
      const { data: cd } = await getComments(id)
      expandedComments.value[id] = cd.success ? (cd.data || []) : []
    } else {
      ElMessage.error(data.message || '评论失败')
    }
  } catch {
    ElMessage.error('评论失败')
  } finally {
    submittingComment.value[id] = false
  }
}

function openReportDialog(targetType, targetId) {
  reportTarget.value = { targetType, targetId }
  reportReason.value = ''
  reportDialogVisible.value = true
}

async function submitReport() {
  if (!reportReason.value.trim()) { ElMessage.warning('请填写举报原因'); return }
  submittingReport.value = true
  try {
    const { data } = await reportContent({
      targetType: reportTarget.value.targetType,
      targetId: reportTarget.value.targetId,
      reason: reportReason.value,
    })
    if (data.success) {
      ElMessage.success('举报已提交，管理员将审核处理')
      reportDialogVisible.value = false
    } else {
      ElMessage.error(data.message || '举报失败')
    }
  } catch {
    ElMessage.error('举报失败')
  } finally {
    submittingReport.value = false
  }
}

function avatarLetter(post) {
  return (post.authorNickname || '?')[0].toUpperCase()
}

function isMyPost(post) {
  return userStore.userInfo?.id && post.userId === userStore.userInfo.id
}

onMounted(loadPosts)
</script>

<template>
  <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; flex-wrap:wrap; gap:12px;">
    <h2 class="page-title" style="margin:0;">用户论坛</h2>
    <el-button type="primary" @click="openDialog">
      <el-icon style="margin-right:4px;"><Plus /></el-icon>发布帖子
    </el-button>
  </div>

  <!-- 筛选栏 -->
  <div style="display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap;">
    <el-select v-model="filterCategory" placeholder="全部类型" clearable style="width:180px;" @change="loadPosts">
      <el-option v-for="item in FORUM_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
    <el-input
      v-model="filterKeyword"
      placeholder="搜索标题或内容"
      clearable
      style="width:220px;"
      @keyup.enter="loadPosts"
      @clear="loadPosts"
    >
      <template #suffix>
        <el-icon style="cursor:pointer;" @click="loadPosts"><Search /></el-icon>
      </template>
    </el-input>
  </div>

  <div v-loading="loadingPosts">
    <el-empty v-if="!loadingPosts && posts.length === 0" description="暂无帖子，点击右上角发第一篇吧" />

    <div style="display:flex; flex-direction:column; gap:16px;">
      <el-card v-for="post in posts" :key="post.id" shadow="hover">
        <!-- 作者信息 -->
        <div style="display:flex; align-items:center; gap:10px; margin-bottom:12px;">
          <el-avatar :size="36" :src="post.authorAvatarUrl || undefined" style="background:#3b82f6; font-size:15px; flex-shrink:0;">
            {{ avatarLetter(post) }}
          </el-avatar>
          <div>
            <div style="font-weight:600; font-size:14px;">{{ post.authorNickname }}</div>
            <div style="font-size:12px; color:#9ca3af;">{{ post.createdAt }}</div>
          </div>
          <el-tag size="small" type="success" style="margin-left:auto;">{{ post.category }}</el-tag>
        </div>

        <!-- 内容 -->
        <div>
          <div style="font-size:16px; font-weight:600; margin-bottom:6px;">{{ post.title }}</div>
          <div v-if="post.tags" style="margin-bottom:8px;">
            <el-tag
              v-for="tag in post.tags.split(',')"
              :key="tag"
              size="small"
              type="info"
              style="margin-right:4px;"
            >{{ tag.trim() }}</el-tag>
          </div>
          <p style="margin:0 0 10px; color:#374151; white-space:pre-wrap; word-break:break-all; font-size:14px;">{{ post.content }}</p>
          <el-image
            v-if="post.imageUrl"
            :src="post.imageUrl"
            style="width:100%; max-height:360px; border-radius:10px; display:block;"
            fit="cover"
            :preview-src-list="[post.imageUrl]"
          />
        </div>

        <!-- 操作栏 -->
        <div style="display:flex; gap:4px; margin-top:12px; padding-top:10px; border-top:1px solid #f1f5f9; align-items:center;">
          <el-button
            :type="post.likedByCurrentUser ? 'primary' : 'default'"
            link
            @click="handleLike(post)"
          >
            <el-icon style="margin-right:4px;"><Pointer /></el-icon>
            {{ post.likeCount || 0 }} 点赞
          </el-button>
          <el-button link @click="toggleComments(post)">
            <el-icon style="margin-right:4px;"><ChatDotRound /></el-icon>
            {{ post.commentCount || 0 }} 评论
          </el-button>
          <el-button
            v-if="!isMyPost(post)"
            type="warning"
            link
            style="margin-left:auto;"
            @click="openReportDialog('POST', post.id)"
          >
            <el-icon style="margin-right:4px;"><Warning /></el-icon>举报
          </el-button>
        </div>

        <!-- 评论区 -->
        <div v-if="expandedComments[post.id]" style="margin-top:12px; background:#f8fafc; border-radius:8px; padding:12px;">
          <div
            v-for="c in expandedComments[post.id]"
            :key="c.id"
            style="display:flex; gap:8px; margin-bottom:10px; align-items:flex-start;"
          >
            <el-avatar :size="28" :src="c.authorAvatarUrl || undefined" style="background:#10b981; font-size:12px; flex-shrink:0;">
              {{ (c.authorNickname || '?')[0].toUpperCase() }}
            </el-avatar>
            <div style="flex:1; min-width:0;">
              <div style="display:flex; align-items:center; gap:6px;">
                <span style="font-weight:600; font-size:13px;">{{ c.authorNickname }}</span>
                <span style="font-size:11px; color:#9ca3af;">{{ c.createdAt }}</span>
                <el-button
                  type="warning"
                  link
                  size="small"
                  style="margin-left:auto; font-size:11px;"
                  @click="openReportDialog('COMMENT', c.id)"
                >举报</el-button>
              </div>
              <span style="font-size:13px; color:#374151;">{{ c.content }}</span>
            </div>
          </div>
          <el-empty v-if="expandedComments[post.id].length === 0" description="还没有评论，来发第一条吧" :image-size="50" />
          <div style="display:flex; gap:8px; margin-top:10px;">
            <el-input
              v-model="commentInputs[post.id]"
              placeholder="写下你的评论..."
              size="small"
              clearable
              @keyup.enter="submitComment(post)"
            />
            <el-button type="primary" size="small" :loading="submittingComment[post.id]" @click="submitComment(post)">发送</el-button>
          </div>
        </div>
      </el-card>
    </div>
  </div>

  <!-- 发布帖子弹窗 -->
  <el-dialog v-model="dialogVisible" title="发布帖子" width="520px" :close-on-click-modal="false" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入标题" clearable />
      </el-form-item>
      <el-form-item label="类型" prop="category">
        <el-select v-model="form.category" placeholder="请选择帖子类型" style="width:100%;">
          <el-option v-for="item in FORUM_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-input v-model="form.tags" placeholder="减脂, 跑步, 低脂餐（逗号分隔）" clearable />
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="5" placeholder="分享你的健康心得..." />
      </el-form-item>
      <el-form-item label="配图">
        <div style="display:flex; align-items:center; gap:12px; flex-wrap:wrap;">
          <el-upload
            :action="`${API_BASE}/uploads/images`"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUpload"
            accept="image/jpeg,image/png,image/webp"
          >
            <el-button>{{ form.imageUrl ? '重新上传' : '上传图片' }}</el-button>
          </el-upload>
          <el-image v-if="form.imageUrl" :src="form.imageUrl" style="width:72px;height:72px;border-radius:6px;" fit="cover" :preview-src-list="[form.imageUrl]" />
          <el-button v-if="form.imageUrl" type="danger" link @click="form.imageUrl = ''">移除</el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="publishing" @click="publish">发布</el-button>
    </template>
  </el-dialog>

  <!-- 举报弹窗 -->
  <el-dialog v-model="reportDialogVisible" title="提交举报" width="420px" :close-on-click-modal="false" destroy-on-close>
    <el-input v-model="reportReason" type="textarea" :rows="3" placeholder="请说明举报原因（虚假宣传、危险建议、不实信息等）" />
    <template #footer>
      <el-button @click="reportDialogVisible = false">取消</el-button>
      <el-button type="warning" :loading="submittingReport" @click="submitReport">提交举报</el-button>
    </template>
  </el-dialog>
</template>
