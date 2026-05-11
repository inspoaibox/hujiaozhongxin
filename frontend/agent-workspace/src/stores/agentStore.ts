import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AgentSummary } from '@/types/call'

export const useAgentStore = defineStore('agent', () => {
  const agents = ref<AgentSummary[]>([
    { id: 1001, agentNo: 'A001', realName: '张三', status: 'IDLE' },
    { id: 1002, agentNo: 'A002', realName: '李四', status: 'IDLE' },
    { id: 1003, agentNo: 'A003', realName: '王五', status: 'TALKING' }
  ])

  const currentAgent = ref({
    id: Number(localStorage.getItem('agentId') || 1000),
    agentNo: localStorage.getItem('username') || 'A000',
    realName: localStorage.getItem('realName') || '当前座席',
    status: 'IDLE',
    role: localStorage.getItem('role') || 'AGENT'
  })

  const idleAgents = computed(() => agents.value.filter(agent => agent.status === 'IDLE'))

  function updateAgentStatus(agentId: number, status: string) {
    const id = Number(agentId)
    const agent = agents.value.find(item => item.id === id)
    if (agent) {
      agent.status = status
    }
    if (currentAgent.value.id === id) {
      currentAgent.value.status = status
    }
  }

  function syncCurrentAgentFromStorage() {
    currentAgent.value = {
      ...currentAgent.value,
      id: Number(localStorage.getItem('agentId') || currentAgent.value.id),
      agentNo: localStorage.getItem('username') || currentAgent.value.agentNo,
      realName: localStorage.getItem('realName') || currentAgent.value.realName,
      role: localStorage.getItem('role') || currentAgent.value.role
    }
  }

  return {
    agents,
    currentAgent,
    idleAgents,
    updateAgentStatus,
    syncCurrentAgentFromStorage
  }
})
