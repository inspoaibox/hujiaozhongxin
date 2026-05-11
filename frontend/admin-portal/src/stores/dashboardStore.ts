import { defineStore } from 'pinia'
import { ref } from 'vue'

interface RealtimeMetrics {
  onlineAgents: number
  talkingAgents: number
  idleAgents: number
  wrapupAgents: number
  restAgents: number
  queueSize: number
}

interface TodayStats {
  inboundCalls: number
  outboundCalls: number
  answerRate: number
  avgWaitTime: number
}

interface AgentStatusRow {
  agentNo: string
  realName: string
  skillGroup: string
  status: string
  statusDuration: number
  todayCalls: number
  avgHandleTime: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

async function request<T>(path: string): Promise<T> {
  const token = localStorage.getItem('token')
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }
  })
  if (!response.ok) {
    throw new Error(`API ${path} failed: ${response.status}`)
  }
  const payload = await response.json() as ApiResult<T> | T
  if (payload && typeof payload === 'object' && 'code' in payload) {
    return (payload as ApiResult<T>).data
  }
  return payload as T
}

export const useDashboardStore = defineStore('dashboard', () => {
  const realtimeMetrics = ref<RealtimeMetrics>({
    onlineAgents: 18,
    talkingAgents: 7,
    idleAgents: 8,
    wrapupAgents: 2,
    restAgents: 1,
    queueSize: 4
  })

  const todayStats = ref<TodayStats>({
    inboundCalls: 286,
    outboundCalls: 94,
    answerRate: 93.6,
    avgWaitTime: 18
  })

  const agentStatusList = ref<AgentStatusRow[]>([
    { agentNo: 'A001', realName: '张三', skillGroup: '通用客服', status: 'IDLE', statusDuration: 128, todayCalls: 42, avgHandleTime: 192 },
    { agentNo: 'A002', realName: '李四', skillGroup: '技术支持', status: 'TALKING', statusDuration: 315, todayCalls: 37, avgHandleTime: 246 },
    { agentNo: 'A003', realName: '王五', skillGroup: '投诉处理', status: 'WRAPUP', statusDuration: 86, todayCalls: 29, avgHandleTime: 221 }
  ])

  const trendHours = ref(['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00'])
  const inboundTrend = ref([32, 48, 51, 30, 39, 45, 53, 41])
  const outboundTrend = ref([12, 18, 16, 10, 14, 20, 17, 13])

  async function fetchRealtimeData() {
    try {
      const data = await request<{
        realtimeMetrics: RealtimeMetrics
        todayStats: TodayStats
        agentStatusList: AgentStatusRow[]
        trendHours: string[]
        inboundTrend: number[]
        outboundTrend: number[]
      }>('/reports/realtime-dashboard')

      realtimeMetrics.value = data.realtimeMetrics
      todayStats.value = data.todayStats
      agentStatusList.value = data.agentStatusList
      trendHours.value = data.trendHours
      inboundTrend.value = data.inboundTrend
      outboundTrend.value = data.outboundTrend
    } catch (error) {
      console.warn('实时监控接口不可用，使用本地降级数据:', error)
    }
  }

  return {
    realtimeMetrics,
    todayStats,
    agentStatusList,
    trendHours,
    inboundTrend,
    outboundTrend,
    fetchRealtimeData
  }
})
