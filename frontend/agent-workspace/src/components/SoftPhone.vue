<template>
  <div class="soft-phone" :class="{ 'in-call': isInCall }">
    <!-- 软电话状态栏 -->
    <div class="phone-status-bar">
      <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
      <span v-if="isInCall" class="call-duration">{{ formatDuration(callDuration) }}</span>
    </div>

    <!-- 来电弹屏 -->
    <el-dialog
      v-model="showIncomingCall"
      title="来电"
      width="360px"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <div class="incoming-call-info">
        <el-avatar :size="64" :icon="User" />
        <div class="caller-info">
          <div class="caller-name">{{ incomingCall?.customerName || '未知来电' }}</div>
          <div class="caller-number">{{ incomingCall?.callerNumber }}</div>
          <el-tag v-if="incomingCall?.isVip" type="warning" size="small">VIP客户</el-tag>
        </div>
      </div>
      <template #footer>
        <el-button type="success" @click="answerCall" :icon="Phone">接听</el-button>
        <el-button type="danger" @click="rejectCall" :icon="CircleCloseFilled">拒接</el-button>
      </template>
    </el-dialog>

    <!-- 通话中控制面板 -->
    <div v-if="isInCall" class="call-controls">
      <div class="customer-card">
        <div class="customer-name">{{ currentCall?.customerName || (currentCall?.callType === 'OUTBOUND' ? '外呼客户' : '未知客户') }}</div>
        <div class="customer-phone">{{ currentCall?.callerNumber || currentCall?.calledNumber }}</div>
        <el-tag v-if="currentCall?.isVip" type="warning" size="small">VIP</el-tag>
      </div>

      <div class="control-buttons">
        <el-tooltip content="保持">
          <el-button
            :type="isHolding ? 'warning' : 'default'"
            circle
            :icon="isHolding ? VideoPlay : VideoPause"
            :disabled="!canToggleHold"
            @click="toggleHold"
          />
        </el-tooltip>

        <el-tooltip content="静音">
          <el-button
            :type="isMuted ? 'warning' : 'default'"
            circle
            :icon="isMuted ? Mute : Microphone"
            :disabled="!canControlLiveCall"
            @click="toggleMute"
          />
        </el-tooltip>

        <el-tooltip content="转接">
          <el-button circle :icon="Share" :disabled="!canControlLiveCall" @click="showTransferDialog = true" />
        </el-tooltip>

        <el-tooltip content="三方通话">
          <el-button circle :icon="Connection" :disabled="!canControlLiveCall" @click="showConferenceDialog = true" />
        </el-tooltip>

        <el-tooltip content="挂断">
          <el-button type="danger" circle :icon="CircleCloseFilled" @click="hangupCall" />
        </el-tooltip>
      </div>
    </div>

    <!-- 拨号盘 -->
    <div v-if="!isInCall" class="dialpad">
      <el-input
        v-model="dialNumber"
        placeholder="输入电话号码"
        class="dial-input"
        clearable
      >
        <template #append>
          <el-button type="primary" :icon="Phone" @click="makeCall">拨打</el-button>
        </template>
      </el-input>

      <div class="keypad">
        <el-button
          v-for="key in dialKeys"
          :key="key"
          class="key-btn"
          @click="dialNumber += key"
        >{{ key }}</el-button>
      </div>
    </div>

    <!-- 转接对话框 -->
    <el-dialog v-model="showTransferDialog" title="转接呼叫" width="400px">
      <el-select v-model="transferTarget" placeholder="选择目标座席" filterable>
        <el-option
          v-for="agent in idleAgents"
          :key="agent.id"
          :label="`${agent.realName} (${agent.agentNo})`"
          :value="agent.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="showTransferDialog = false">取消</el-button>
        <el-button type="primary" @click="transferCall">确认转接</el-button>
      </template>
    </el-dialog>

    <!-- 三方通话对话框 -->
    <el-dialog v-model="showConferenceDialog" title="发起三方通话" width="400px">
      <el-input
        v-model="conferenceNumber"
        placeholder="输入第三方电话号码"
        clearable
      />
      <template #footer>
        <el-button @click="showConferenceDialog = false">取消</el-button>
        <el-button type="primary" @click="startConference">发起三方</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Phone, CircleCloseFilled, Microphone, Mute, Share, Connection, VideoPlay, VideoPause, User } from '@element-plus/icons-vue'
import { useCallStore } from '@/stores/callStore'
import { useAgentStore } from '@/stores/agentStore'
import { ElMessage } from 'element-plus'

const callStore = useCallStore()
const agentStore = useAgentStore()

const dialNumber = ref('')
const showIncomingCall = ref(false)
const showTransferDialog = ref(false)
const showConferenceDialog = ref(false)
const transferTarget = ref<number | null>(null)
const conferenceNumber = ref('')
const isHolding = ref(false)
const isMuted = ref(false)
const callDuration = ref(0)
let durationTimer: ReturnType<typeof setInterval> | null = null

const isInCall = computed(() => callStore.isInCall)
const currentCall = computed(() => callStore.currentCall)
const incomingCall = computed(() => callStore.incomingCall)
const idleAgents = computed(() => agentStore.idleAgents)
const canControlLiveCall = computed(() => currentCall.value?.status === 'ANSWERED')
const canToggleHold = computed(() => currentCall.value?.status === 'ANSWERED' || currentCall.value?.status === 'HOLDING')

const statusLabel = computed(() => {
  if (showIncomingCall.value) return '来电'
  if (currentCall.value?.status === 'INITIATED' || currentCall.value?.status === 'RINGING') return '呼叫中'
  if (currentCall.value?.status === 'HOLDING') return '保持中'
  if (currentCall.value?.status === 'CONFERENCING') return '三方通话'
  if (isInCall.value) return '通话中'
  return '空闲'
})

const statusTagType = computed(() => {
  if (showIncomingCall.value) return 'warning'
  if (currentCall.value?.status === 'INITIATED' || currentCall.value?.status === 'RINGING') return 'warning'
  if (currentCall.value?.status === 'HOLDING') return 'warning'
  if (isInCall.value) return 'success'
  return 'info'
})

const dialKeys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#']

function formatDuration(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return h > 0
    ? `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
    : `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

async function makeCall() {
  if (!dialNumber.value) {
    ElMessage.warning('请输入电话号码')
    return
  }
  try {
    await callStore.makeOutboundCall(dialNumber.value)
    dialNumber.value = ''
    if (isInCall.value) {
      callDuration.value = 0
      startDurationTimer()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '呼出失败')
  }
}

async function answerCall() {
  showIncomingCall.value = false
  try {
    await callStore.answerCall()
    if (isInCall.value) {
      callDuration.value = 0
      startDurationTimer()
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '接听失败')
  }
}

function rejectCall() {
  showIncomingCall.value = false
  callStore.rejectCall()
}

async function hangupCall() {
  try {
    await callStore.hangupCall()
    stopDurationTimer()
    callDuration.value = 0
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '挂断失败')
  }
}

async function toggleHold() {
  const nextHolding = !isHolding.value
  try {
    await callStore.toggleHold(nextHolding)
    isHolding.value = nextHolding
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保持操作失败')
  }
}

function toggleMute() {
  isMuted.value = !isMuted.value
  // SIP.js 静音控制
}

async function transferCall() {
  if (!transferTarget.value) return
  try {
    await callStore.transferCall(transferTarget.value)
    showTransferDialog.value = false
    stopDurationTimer()
    callDuration.value = 0
    transferTarget.value = null
    ElMessage.success('转接成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '转接失败')
  }
}

async function startConference() {
  if (!conferenceNumber.value) {
    ElMessage.warning('请输入第三方电话号码')
    return
  }
  try {
    await callStore.conferenceCall(conferenceNumber.value)
    showConferenceDialog.value = false
    conferenceNumber.value = ''
    ElMessage.success('三方通话已发起')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '三方通话发起失败')
  }
}

function startDurationTimer() {
  stopDurationTimer()
  durationTimer = setInterval(() => {
    callDuration.value++
  }, 1000)
}

function stopDurationTimer() {
  if (durationTimer) {
    clearInterval(durationTimer)
    durationTimer = null
  }
}

// 监听来电事件
onMounted(() => {
  callStore.$subscribe((mutation, state) => {
    if (state.incomingCall) {
      showIncomingCall.value = true
    }
  })
})

onUnmounted(() => {
  stopDurationTimer()
})
</script>

<style scoped>
.soft-phone {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.phone-status-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.call-duration {
  font-size: 14px;
  color: #67c23a;
  font-weight: bold;
}

.incoming-call-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 0;
}

.caller-info {
  flex: 1;
}

.caller-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 4px;
}

.caller-number {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.call-controls {
  padding: 12px 0;
}

.customer-card {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}

.customer-name {
  font-size: 16px;
  font-weight: bold;
}

.customer-phone {
  font-size: 13px;
  color: #909399;
  margin: 4px 0;
}

.control-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.dialpad {
  padding: 8px 0;
}

.dial-input {
  margin-bottom: 12px;
}

.keypad {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.key-btn {
  height: 40px;
  font-size: 16px;
}
</style>
