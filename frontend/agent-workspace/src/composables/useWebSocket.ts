import { ref, onUnmounted } from 'vue'
import { useCallStore } from '@/stores/callStore'
import { useAgentStore } from '@/stores/agentStore'
import { ElNotification } from 'element-plus'
import type { IncomingCallInfo } from '@/types/call'

/**
 * WebSocket 实时消息 Composable
 * 作者：深圳市千牛云科技有限公司
 */
export function useWebSocket() {
  const socket = ref<WebSocket | null>(null)
  const isConnected = ref(false)
  const callStore = useCallStore()
  const agentStore = useAgentStore()
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null

  function connect(token: string) {
    disconnect()

    const baseUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8889/ws/agent'
    const url = new URL(baseUrl, window.location.href.replace(/^http/, 'ws'))
    if (token) url.searchParams.set('token', token)
    url.searchParams.set('agentId', localStorage.getItem('agentId') || '1000')
    url.searchParams.set('role', localStorage.getItem('role') || 'AGENT')

    socket.value = new WebSocket(url)

    socket.value.onopen = () => {
      isConnected.value = true
      console.log('WebSocket 连接成功')
      heartbeatTimer = setInterval(() => {
        if (socket.value?.readyState === WebSocket.OPEN) {
          socket.value.send('ping')
        }
      }, 30000)
    }

    socket.value.onclose = () => {
      isConnected.value = false
      if (heartbeatTimer) {
        clearInterval(heartbeatTimer)
        heartbeatTimer = null
      }
      console.log('WebSocket 连接断开')
    }

    socket.value.onerror = error => {
      console.warn('WebSocket 连接异常', error)
    }

    socket.value.onmessage = event => {
      if (event.data === 'pong') return

      let message: { type?: string; data?: Record<string, unknown> }
      try {
        message = JSON.parse(event.data)
      } catch (error) {
        console.warn('收到无法解析的 WebSocket 消息:', event.data, error)
        return
      }
      const type = message.type
      const data = message.data || {}

      if (type === 'CALL_EVENT' && data.eventType === 'INCOMING_CALL') {
        callStore.handleIncomingCall(data as IncomingCallInfo)
      }

      if (type === 'AGENT_STATUS_EVENT') {
        agentStore.updateAgentStatus(Number(data.agentId), String(data.newStatus || 'OFFLINE'))
      }

      if (type === 'TICKET_ASSIGNED') {
        ElNotification({
          title: '新工单',
          message: `您有新工单待处理: ${data.ticketNo}`,
          type: 'info',
          duration: 5000
        })
      }

      if (type === 'WRAPUP_TIMEOUT') {
        ElNotification({
          title: '提醒',
          message: '整理时间已超过3分钟，请及时处理',
          type: 'warning',
          duration: 0
        })
      }
    }
  }

  function disconnect() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
    socket.value?.close()
    socket.value = null
    isConnected.value = false
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    socket,
    isConnected,
    connect,
    disconnect
  }
}
