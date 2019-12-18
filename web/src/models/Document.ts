import { AxiosInstance } from 'axios'
import dayjs from 'dayjs'
import { halPageResourceFromJSON, halResourceFromJSON, idLookupFromJSON } from './HalResource'
import { labelFromJSON } from './Label'

export interface Document {
  id: string
  userId: string
  blobId: string
  revisionNumber: number
  name: string
  labelIds: string[]
  contentType: string
  createdAt: dayjs.Dayjs
  updatedAt: dayjs.Dayjs
  size: number
}

export function documentFromJson(js: any): Document {
  return {
    ...js,
    createdAt: dayjs(js.createdAt),
    updatedAt: dayjs(js.updatedAt),
  }
}

export const searchDocuments = async (http: AxiosInstance, query: string, from: number, limit: number) => {
  const res = await http.get('/api/documents/search', {
    params: {
      query,
      from,
      limit,
    },
  })
  return halPageResourceFromJSON(res.data, documentFromJson, {
    labels: idLookupFromJSON(labelFromJSON),
  })
}

export const listDocuments = async (http: AxiosInstance, from: number, limit: number) => {
  const res = await http.get('/api/documents', {
    params: {
      from,
      limit,
    },
  })
  return halPageResourceFromJSON(res.data, documentFromJson, {
    labels: idLookupFromJSON(labelFromJSON),
  })
}

export const retrieveDocument = async (http: AxiosInstance, documentId: string) => {
  const res = await http.get(`/api/documents/${documentId}`)
  return halResourceFromJSON(res.data, documentFromJson, {
    labels: idLookupFromJSON(labelFromJSON),
  })
}

export const createDocument = async (http: AxiosInstance, name: string, contentType: string, data: any) => {
  const res = await http.post('/api/documents', data, {
    params: {
      name,
    },
    headers: {
      'content-type': contentType,
    },
  })
  return halResourceFromJSON(res.data, documentFromJson, {
    labels: idLookupFromJSON(labelFromJSON),
  })
}
