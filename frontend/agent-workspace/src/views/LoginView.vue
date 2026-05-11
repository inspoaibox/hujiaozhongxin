<template>
  <main class="login-page">
    <section class="login-panel">
      <p class="eyebrow">千牛云呼叫中心</p>
      <h1>座席登录</h1>
      <el-form class="login-form" @submit.prevent="submitLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="账号" size="large" autocomplete="username" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="password"
            placeholder="密码"
            size="large"
            type="password"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="submitLogin">
          登录
        </el-button>
      </el-form>
      <p class="hint">默认管理员：admin / Admin@2025，上线前请修改密码。</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/authApi'
import { useAgentStore } from '@/stores/agentStore'

const router = useRouter()
const agentStore = useAgentStore()
const username = ref(localStorage.getItem('username') || 'admin')
const password = ref('')
const loading = ref(false)

async function submitLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const response = await login(username.value, password.value)
    localStorage.setItem('token', response.token)
    localStorage.setItem('username', response.username)
    localStorage.setItem('realName', response.realName)
    localStorage.setItem('role', response.role || 'AGENT')
    localStorage.setItem('agentId', String(response.userId))
    agentStore.syncCurrentAgentFromStorage()
    await router.push('/')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f5f7fa;
}

.login-panel {
  width: min(100%, 380px);
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 28px;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.08);
}

.eyebrow {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}

h1 {
  font-size: 26px;
  color: #303133;
  margin-bottom: 22px;
}

.login-form {
  display: grid;
  gap: 4px;
}

.submit-btn {
  width: 100%;
}

.hint {
  margin-top: 16px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
