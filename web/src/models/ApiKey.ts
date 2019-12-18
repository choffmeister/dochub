import { AxiosInstance } from 'axios'
import dayjs from 'dayjs'
import { halPageResourceFromJSON } from './HalResource'
import { Scope } from './Principal'

export interface ApiKey {
  id: string
  userId: string
  name: string
  secret: string
  scopes?: Scope[]
  createdAt: dayjs.Dayjs
  lastUsedAt: dayjs.Dayjs
}

export function apiKeyFromJSON(js: any): ApiKey {
  return {
    ...js,
    createdAt: dayjs(js.createdAt),
    lastUsedAt: js.lastUsedAt ? dayjs(js.lastUsedAt) : undefined,
  }
}

export const createApiKey = async (http: AxiosInstance, name: string, scopes?: Scope[]) => {
  const res = await http.post('/api/api-keys', null, {
    params: {
      name,
      scopes,
    },
  })
  return apiKeyFromJSON(res.data)
}

export const listApiKeys = async (http: AxiosInstance, from: number, limit: number) => {
  const res = await http.get('/api/api-keys', {
    params: {
      from,
      limit,
    },
  })
  return halPageResourceFromJSON(res.data, apiKeyFromJSON, {})
}

export const deleteApiKey = async (http: AxiosInstance, apiKeyId: string) => {
  await http.delete(`/api/api-keys/${apiKeyId}`)
}
