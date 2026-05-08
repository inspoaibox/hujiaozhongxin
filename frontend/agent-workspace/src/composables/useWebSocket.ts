import { ref, onMounted, onUnmounted } from 'vue'
import { io, Socket } from 'socket.io-client'
import { useCallStore } from '@/stores/callStore'
import { useAgentStore } from '@/stores/agentStore'
import { ElNotification } from 'element-plus'

/**
 * WebSocket 实时消息 Composable
 * 作者：深圳市千牛云科技有限公司
 */
export function useWebSocket() {
  const socket = ref<Socket | null>(null)
  const isConnected = ref(false)
  const callStore = useCallStore()
  const agentStore = useAgentStore()

  function connect(token: string) {
    socket.value = io(import.meta.env.VITE_WS_URL || 'ws://localhost:8889', {
      auth: { token },
      transports: ['websocket'],
      reconnection: true,
      reconnectionDelay: 3000,
      reconnectionAttempts: 10
    })

    socket.value.on('connect', () => {
      isConnected.value = true
      console.log('WebSocket 连接成功')
    })

    socket.value.on('disconnect', () => {
      isConnected.value = false
      console.log('WebSocket 连接断开')
    })

    // 来电事件
    socket.value.on('CALL_EVENT', (data: any) => {
      if (data.eventType === 'INCOMING_CALL') {
        callStore.handleIncomingCall(data)
      }
    })

    // 座席状态变更
    socket.value.on('AGENT_STATUS_EVENT', (data: any) => {
      agentStore.updateAgentStatus(data.agentId, data.newStatus)
    })

    // 工单通知
    socket.value.on('TICKET_ASSIGNED', (data: any) => {
      ElNotification({
        title: '新工单',
        message: `您有新工单待处理: ${data.ticketNo}`,
        type: 'info',
        duration: 5000
      })
    })

    // 整理超时提醒
    socket.value.on('WRAPUP_TIMEOUT', () => {
      ElNotification({
        title: '提醒',
        message: '整理时间已超过3分钟，请及时处理',
        type: 'warning',
        duration: 0  // 不自动关闭
      })
    })

    // 心跳
    setInterval(() => {
      if (socket.value?.connected) {
        socket.value.emit('ping')
      }
    }, 30000)
  }

  function disconnect() {
    socket.value?.disconnect()
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
