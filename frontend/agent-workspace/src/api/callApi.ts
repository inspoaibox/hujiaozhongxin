import type { Call } from '@/types/call'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

class ApiError extends Error {}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token')
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers
    },
    ...options
  })

  if (!response.ok) {
    let message = `API ${path} failed: ${response.status}`
    try {
      const payload = await response.json() as Partial<ApiResult<T>>
      message = payload.message || message
    } catch {
      // 响应体不是 JSON 时保留 HTTP 状态信息
    }
    throw new ApiError(message)
  }

  const payload = await response.json() as ApiResult<T> | T
  if (payload && typeof payload === 'object' && 'code' in payload) {
    const result = payload as ApiResult<T>
    if (result.code !== 200) {
      throw new ApiError(result.message || '请求失败')
    }
    return result.data
  }
  return payload as T
}

function mockCall(phone: string): Call {
  return {
    id: `local-${Date.now()}`,
    callType: 'OUTBOUND',
    status: 'ANSWERED',
    calledNumber: phone,
    customerName: '本地测试客户',
    createdAt: new Date().toISOString()
  }
}

async function callOrMock<T>(operation: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await operation()
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    console.warn('后端接口不可用，使用本地降级数据:', error)
    return fallback
  }
}

export const callApi = {
  createOutboundCall(phone: string) {
    return callOrMock(
      () => request<Call>('/calls/outbound', {
        method: 'POST',
        body: JSON.stringify({ phone })
      }),
      mockCall(phone)
    )
  },

  answerCall(callId: string) {
    return callOrMock(
      () => request<void>(`/calls/${callId}/answer`, { method: 'POST' }),
      undefined
    )
  },

  hangupCall(callId: string) {
    return callOrMock(
      () => request<void>(`/calls/${callId}/hangup`, { method: 'POST' }),
      undefined
    )
  },

  holdCall(callId: string) {
    return callOrMock(
      () => request<void>(`/calls/${callId}/hold`, { method: 'POST' }),
      undefined
    )
  },

  unholdCall(callId: string) {
    return callOrMock(
      () => request<void>(`/calls/${callId}/unhold`, { method: 'POST' }),
      undefined
    )
  },

  transferCall(callId: string, targetAgentId: number) {
    return callOrMock(
      () => request<void>(`/calls/${callId}/transfer`, {
        method: 'POST',
        body: JSON.stringify({ targetAgentId })
      }),
      undefined
    )
  },

  conferenceCall(callId: string, thirdPartyNumber: string) {
    return callOrMock(
      () => request<void>(`/calls/${callId}/conference`, {
        method: 'POST',
        body: JSON.stringify({ thirdPartyNumber })
      }),
      undefined
    )
  },

  updateSummary(callId: string, summary: string) {
    return callOrMock(
      () => request<void>(`/calls/${callId}/summary`, {
        method: 'PUT',
        body: JSON.stringify({ summary })
      }),
      undefined
    )
  }
}
