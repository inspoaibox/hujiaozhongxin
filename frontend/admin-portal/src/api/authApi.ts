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
  const payload = await response.json() as ApiResult<LoginResponse>
  if (!response.ok || payload.code !== 200) {
    throw new Error(payload.message || '登录失败')
  }
  return payload.data
}
