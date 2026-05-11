import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { callApi } from '@/api/callApi'
import type { Call, IncomingCallInfo } from '@/types/call'

/**
 * 呼叫状态管理 Store
 * 作者：深圳市千牛云科技有限公司
 */
export const useCallStore = defineStore('call', () => {
  const currentCall = ref<Call | null>(null)
  const incomingCall = ref<IncomingCallInfo | null>(null)
  const callHistory = ref<Call[]>([])

  const isInCall = computed(() => currentCall.value !== null &&
    ['INITIATED', 'RINGING', 'ANSWERED', 'HOLDING', 'TRANSFERRING', 'CONFERENCING'].includes(currentCall.value.status))

  /**
   * 发起呼出呼叫
   */
  async function makeOutboundCall(phone: string) {
    const call = await callApi.createOutboundCall(phone)
    currentCall.value = call
  }

  /**
   * 接听来电
   */
  async function answerCall() {
    if (!incomingCall.value) return
    await callApi.answerCall(incomingCall.value.callId)
    currentCall.value = {
      ...incomingCall.value,
      id: incomingCall.value.callId,
      callType: 'INBOUND',
      status: 'ANSWERED',
      answerAt: new Date().toISOString()
    } as Call
    incomingCall.value = null
  }

  /**
   * 拒接来电
   */
  function rejectCall() {
    incomingCall.value = null
  }

  /**
   * 挂断呼叫
   */
  async function hangupCall() {
    if (!currentCall.value) return
    await callApi.hangupCall(currentCall.value.id)
    callHistory.value.unshift(currentCall.value)
    currentCall.value = null
  }

  /**
   * 保持/恢复通话
   */
  async function toggleHold(hold: boolean) {
    if (!currentCall.value) return
    if (hold) {
      await callApi.holdCall(currentCall.value.id)
      currentCall.value.status = 'HOLDING'
    } else {
      await callApi.unholdCall(currentCall.value.id)
      currentCall.value.status = 'ANSWERED'
    }
  }

  /**
   * 转接呼叫
   */
  async function transferCall(targetAgentId: number) {
    if (!currentCall.value) return
    await callApi.transferCall(currentCall.value.id, targetAgentId)
    currentCall.value = null
  }

  /**
   * 发起三方通话
   */
  async function conferenceCall(thirdPartyNumber: string) {
    if (!currentCall.value) return
    await callApi.conferenceCall(currentCall.value.id, thirdPartyNumber)
    if (currentCall.value) {
      currentCall.value.status = 'CONFERENCING'
    }
  }

  /**
   * 处理来电推送（WebSocket 触发）
   */
  function handleIncomingCall(callInfo: IncomingCallInfo) {
    incomingCall.value = callInfo
  }

  /**
   * 更新通话小结
   */
  async function updateSummary(summary: string) {
    if (!currentCall.value) return
    await callApi.updateSummary(currentCall.value.id, summary)
  }

  return {
    currentCall,
    incomingCall,
    callHistory,
    isInCall,
    makeOutboundCall,
    answerCall,
    rejectCall,
    hangupCall,
    toggleHold,
    transferCall,
    conferenceCall,
    handleIncomingCall,
    updateSummary
  }
})
