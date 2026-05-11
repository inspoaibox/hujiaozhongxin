interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  realName: string
  role: string
  expiresIn: number
}

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

export async function login(username: string, password: string): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  })
  const payload = await parseApiResult<LoginResponse>(response)
  if (!response.ok || payload.code !== 200) {
    throw new Error(payload.message || '登录失败')
  }
  return payload.data
}

async function parseApiResult<T>(response: Response): Promise<ApiResult<T>> {
  const text = await response.text()
  const contentType = response.headers.get('content-type') || ''

  if (!text) {
    return {
      code: response.status,
      message: response.ok ? '接口返回为空' : `登录接口异常：HTTP ${response.status}`,
      data: null as T
    }
  }

  if (!contentType.includes('application/json')) {
    return {
      code: response.status || 500,
      message: `登录接口没有返回 JSON，请检查 API 网关和认证服务。HTTP ${response.status}`,
      data: null as T
    }
  }

  try {
    return JSON.parse(text) as ApiResult<T>
  } catch {
    return {
      code: response.status || 500,
      message: '登录接口返回格式异常，请检查后端服务日志',
      data: null as T
    }
  }
}
