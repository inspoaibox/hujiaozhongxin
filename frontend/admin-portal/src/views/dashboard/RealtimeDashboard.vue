<template>
  <div class="realtime-dashboard" :class="{ 'fullscreen': isFullscreen }">
    <!-- 顶部标题栏 -->
    <div class="dashboard-header">
      <h2>千牛云呼叫中心 - 实时监控</h2>
      <div class="header-actions">
        <span class="update-time">最后更新: {{ lastUpdateTime }}</span>
        <el-button :icon="FullScreen" circle @click="toggleFullscreen" />
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </div>

    <!-- 关键指标卡片 -->
    <el-row :gutter="16" class="metrics-row">
      <el-col :span="6">
        <el-card class="metric-card">
          <div class="metric-value">{{ metrics.onlineAgents }}</div>
          <div class="metric-label">在线座席</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card talking">
          <div class="metric-value">{{ metrics.talkingAgents }}</div>
          <div class="metric-label">通话中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card idle">
          <div class="metric-value">{{ metrics.idleAgents }}</div>
          <div class="metric-label">空闲座席</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card" :class="{ 'alert': metrics.queueSize > 10 }">
          <div class="metric-value">{{ metrics.queueSize }}</div>
          <div class="metric-label">
            队列等待
            <el-tag v-if="metrics.queueSize > 10" type="danger" size="small">预警</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 今日统计 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-statistic title="今日呼入" :value="todayStats.inboundCalls" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="今日呼出" :value="todayStats.outboundCalls" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="接通率" :value="todayStats.answerRate" suffix="%" :precision="1" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="平均等待(秒)" :value="todayStats.avgWaitTime" :precision="0" />
      </el-col>
    </el-row>

    <!-- 座席状态列表 -->
    <el-card class="agent-status-card">
      <template #header>
        <span>座席实时状态</span>
      </template>
      <el-table :data="agentStatusList" stripe size="small" max-height="300">
        <el-table-column prop="agentNo" label="工号" width="80" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="skillGroup" label="技能组" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态时长" width="100">
          <template #default="{ row }">
            {{ formatDuration(row.statusDuration) }}
          </template>
        </el-table-column>
        <el-table-column prop="todayCalls" label="今日接听" width="90" />
        <el-table-column prop="avgHandleTime" label="平均处理(秒)" width="110" />
      </el-table>
    </el-card>

    <!-- 呼叫趋势图 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="16">
        <el-card>
          <template #header>今日呼叫趋势</template>
          <div ref="callTrendChart" style="height: 250px;" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>座席状态分布</template>
          <div ref="agentStatusChart" style="height: 250px;" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { FullScreen } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '@/stores/dashboardStore'
import dayjs from 'dayjs'

const dashboardStore = useDashboardStore()
const router = useRouter()
const isFullscreen = ref(false)
const callTrendChart = ref<HTMLElement | null>(null)
const agentStatusChart = ref<HTMLElement | null>(null)
let trendChartInstance: echarts.ECharts | null = null
let statusChartInstance: echarts.ECharts | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

const lastUpdateTime = ref(dayjs().format('HH:mm:ss'))
const metrics = computed(() => dashboardStore.realtimeMetrics)
const todayStats = computed(() => dashboardStore.todayStats)
const agentStatusList = computed(() => dashboardStore.agentStatusList)

function getStatusTagType(status: string) {
  const map: Record<string, string> = {
    IDLE: 'success', TALKING: 'primary', WRAPUP: 'warning',
    REST: 'info', OFFLINE: 'danger'
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    IDLE: '空闲', TALKING: '通话中', WRAPUP: '整理',
    REST: '休息', OFFLINE: '离线'
  }
  return map[status] || status
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  setTimeout(resizeCharts, 0)
}

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}

function resizeCharts() {
  trendChartInstance?.resize()
  statusChartInstance?.resize()
}

function initCharts() {
  if (callTrendChart.value) {
    trendChartInstance = echarts.init(callTrendChart.value)
    trendChartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['呼入', '呼出'] },
      xAxis: { type: 'category', data: dashboardStore.trendHours },
      yAxis: { type: 'value' },
      series: [
        { name: '呼入', type: 'line', smooth: true, data: dashboardStore.inboundTrend, itemStyle: { color: '#409eff' } },
        { name: '呼出', type: 'line', smooth: true, data: dashboardStore.outboundTrend, itemStyle: { color: '#67c23a' } }
      ]
    })
  }

  if (agentStatusChart.value) {
    statusChartInstance = echarts.init(agentStatusChart.value)
    statusChartInstance.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: [
          { value: metrics.value.talkingAgents, name: '通话中', itemStyle: { color: '#409eff' } },
          { value: metrics.value.idleAgents, name: '空闲', itemStyle: { color: '#67c23a' } },
          { value: metrics.value.wrapupAgents, name: '整理', itemStyle: { color: '#e6a23c' } },
          { value: metrics.value.restAgents, name: '休息', itemStyle: { color: '#909399' } }
        ]
      }]
    })
  }
}

async function refreshData() {
  await dashboardStore.fetchRealtimeData()
  lastUpdateTime.value = dayjs().format('HH:mm:ss')
  // 更新图表
  trendChartInstance?.setOption({
    xAxis: { data: dashboardStore.trendHours },
    series: [
      { data: dashboardStore.inboundTrend },
      { data: dashboardStore.outboundTrend }
    ]
  })
  statusChartInstance?.setOption({
    series: [{
      data: [
        { value: metrics.value.talkingAgents, name: '通话中', itemStyle: { color: '#409eff' } },
        { value: metrics.value.idleAgents, name: '空闲', itemStyle: { color: '#67c23a' } },
        { value: metrics.value.wrapupAgents, name: '整理', itemStyle: { color: '#e6a23c' } },
        { value: metrics.value.restAgents, name: '休息', itemStyle: { color: '#909399' } }
      ]
    }]
  })
}

onMounted(async () => {
  await refreshData()
  initCharts()
  window.addEventListener('resize', resizeCharts)
  // 每5秒刷新一次
  refreshTimer = setInterval(refreshData, 5000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', resizeCharts)
  trendChartInstance?.dispose()
  statusChartInstance?.dispose()
})
</script>

<style scoped>
.realtime-dashboard {
  padding: 16px;
  background: #f5f7fa;
  min-height: 100vh;
}

.realtime-dashboard.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: #001529;
  color: #fff;
  overflow-y: auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.update-time {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;
}

.metrics-row, .stats-row, .chart-row {
  margin-bottom: 16px;
}

.metric-card {
  text-align: center;
  cursor: default;
}

.metric-card.alert {
  border-color: #f56c6c;
  background: #fef0f0;
}

.metric-value {
  font-size: 36px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.metric-card.talking .metric-value { color: #409eff; }
.metric-card.idle .metric-value { color: #67c23a; }
.metric-card.alert .metric-value { color: #f56c6c; }

.metric-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.agent-status-card {
  margin-bottom: 16px;
}
</style>
