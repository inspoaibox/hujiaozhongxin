export type CallStatus =
  | 'INITIATED'
  | 'RINGING'
  | 'QUEUED'
  | 'ANSWERED'
  | 'HOLDING'
  | 'TRANSFERRING'
  | 'CONFERENCING'
  | 'COMPLETED'
  | 'ABANDONED'
  | 'FAILED'

export interface Call {
  id: string
  callId?: string
  callType: 'INBOUND' | 'OUTBOUND'
  status: CallStatus
  callerNumber?: string
  calledNumber?: string
  customerName?: string
  isVip?: boolean
  agentId?: number
  customerId?: number
  answerAt?: string
  createdAt?: string
  duration?: number
  summary?: string
}

export interface IncomingCallInfo {
  id?: string
  callId: string
  callType?: 'INBOUND'
  callerNumber: string
  calledNumber?: string
  customerName?: string
  isVip?: boolean
  customerId?: number
}

export interface AgentSummary {
  id: number
  agentNo: string
  realName: string
  status: string
}
