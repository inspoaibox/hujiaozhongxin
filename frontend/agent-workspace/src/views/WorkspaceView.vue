<template>
  <main class="workspace-page">
    <section class="workspace-header">
      <div>
        <p class="eyebrow">座席工作台</p>
        <h1>呼叫处理</h1>
      </div>
      <div class="header-actions">
        <el-tag :type="currentAgent.status === 'IDLE' ? 'success' : 'info'">
          {{ currentAgent.realName }} · {{ statusLabel }}
        </el-tag>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </section>

    <section class="workspace-grid">
      <SoftPhone />

      <div class="side-panel">
        <el-card>
          <template #header>当前提醒</template>
          <el-empty description="暂无待处理提醒" :image-size="80" />
        </el-card>

        <el-card>
          <template #header>空闲座席</template>
          <el-table :data="idleAgents" size="small">
            <el-table-column prop="agentNo" label="工号" width="90" />
            <el-table-column prop="realName" label="姓名" />
          </el-table>
        </el-card>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import SoftPhone from '@/components/SoftPhone.vue'
import { useAgentStore } from '@/stores/agentStore'
import { useWebSocket } from '@/composables/useWebSocket'

const router = useRouter()
const agentStore = useAgentStore()
const { connect, disconnect } = useWebSocket()
const currentAgent = computed(() => agentStore.currentAgent)
const idleAgents = computed(() => agentStore.idleAgents)
const statusLabel = computed(() => currentAgent.value.status === 'IDLE' ? '空闲' : currentAgent.value.status)

onMounted(() => {
  connect(localStorage.getItem('token') || '')
})

onUnmounted(() => {
  disconnect()
})

function logout() {
  disconnect()
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.workspace-page {
  min-height: 100vh;
  padding: 20px;
  background: #f5f7fa;
}

.workspace-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.eyebrow {
  color: #909399;
  font-size: 13px;
  margin-bottom: 4px;
}

h1 {
  font-size: 24px;
  line-height: 1.2;
  color: #303133;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(320px, 420px) 1fr;
  gap: 16px;
  align-items: start;
}

.side-panel {
  display: grid;
  gap: 16px;
}

@media (max-width: 900px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
