export interface Label {
  id: string
  userId: string
  name: string
  color: string
}

export function labelFromJSON(js: any): Label {
  return {
    ...js,
  }
}
