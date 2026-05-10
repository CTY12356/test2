<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请先填写用户名和密码')
    return
  }
  loading.value = true
  try {
    const { data } = await login(form)
    if (!data.success) {
      ElMessage.error(data.message)
      return
    }
    userStore.setUser(data.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '请求失败，请确认后端服务已启动')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-wrap">
    <el-card class="auth-card">
      <div class="auth-logo">
        <span class="logo-icon">💪</span>
        <span class="logo-text">健康管理平台</span>
      </div>
      <h3 class="auth-title">欢迎回来</h3>
      <p class="auth-sub">登录后开始管理您的健康数据</p>

      <el-form label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%;" size="large" @click="submit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        还没有账号？
        <el-button type="primary" link @click="router.push('/register')">立即注册</el-button>
      </div>

      <p class="auth-disclaimer">健康数据仅供参考，不能替代医生或营养师建议。</p>
    </el-card>
  </div>
</template>

<style scoped>
.auth-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f2fe 0%, #f0fdf4 100%);
}
.auth-card {
  width: 420px;
  padding: 16px 8px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.10);
}
.auth-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}
.logo-icon { font-size: 28px; }
.logo-text { font-size: 20px; font-weight: 700; color: #1d4ed8; }
.auth-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px;
  color: #111827;
}
.auth-sub {
  text-align: center;
  color: #6b7280;
  font-size: 13px;
  margin: 0 0 24px;
}
.auth-footer {
  text-align: center;
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
}
.auth-disclaimer {
  text-align: center;
  font-size: 11px;
  color: #9ca3af;
  margin-top: 16px;
  margin-bottom: 0;
}
</style>
