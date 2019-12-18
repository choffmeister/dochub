export type Scope = 'admin' | 'write' | 'read'

export const scopes: Scope[] = ['admin', 'write', 'read']

export interface Principal {
  userId: string
  username: string
  claims: string[]
}

export function principalFromJSON(js: any): Principal {
  return {
    userId: js.sub,
    username: js.username,
    claims: js.claims.split(','),
  }
}
