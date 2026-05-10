<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { getMe, updateMe } from '../api/user'

const userStore = useUserStore()
const profile = ref(null)
const saving = ref(false)
const form = reactive({ nickname: '', avatarUrl: '' })
const uploadHeaders = { Authorization: `Bearer ${userStore.token}` }

async function loadMe() {
  try {
    const { data } = await getMe()
    if (data.success) {
      profile.value = data.data
      form.nickname = data.data.nickname || ''
      form.avatarUrl = data.data.avatarUrl || ''
    }
  } catch {
    ElMessage.error('获取用户信息失败')
  }
}

async function saveProfile() {
  saving.value = true
  try {
    const { data } = await updateMe(form)
    if (data.success) {
      profile.value = data.data
      userStore.updateProfile(data.data)
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(data.message)
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleAvatarUpload(response) {
  if (response.success && response.data?.url) {
    form.avatarUrl = response.data.url
    ElMessage.success('头像上传成功，点击保存生效')
  } else {
    ElMessage.error('头像上传失败')
  }
}

onMounted(loadMe)
</script>

<template>
  <el-card class="form-card">
    <h2 class="page-title">个人中心</h2>

    <template v-if="profile">
      <!-- 头像 -->
      <div style="display:flex; flex-direction:column; align-items:center; margin-bottom:28px; gap:12px;">
        <el-avatar :size="90" :src="form.avatarUrl || undefined" style="font-size:36px; background:#3b82f6;">
          {{ (form.avatarUrl ? '' : (profile.nickname || profile.username || '?')[0].toUpperCase()) }}
        </el-avatar>
        <el-upload
          action="/api/uploads/images"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleAvatarUpload"
          accept="image/jpeg,image/png,image/webp"
        >
          <el-button size="small">更换头像</el-button>
        </el-upload>
      </div>

      <!-- 基本信息 -->
      <el-descriptions :column="1" border style="margin-bottom:20px;">
        <el-descriptions-item label="用户 ID">{{ profile.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ profile.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="profile.role === 'ADMIN' ? 'danger' : 'info'" size="small">
            {{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 编辑昵称 -->
      <el-form label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="设置你的昵称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
        </el-form-item>
      </el-form>
    </template>

    <el-empty v-else description="未获取到用户信息，请重新登录" />
  </el-card>
</template>
