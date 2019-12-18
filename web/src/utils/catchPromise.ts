export type CatchedPromise<T> = { type: 'success'; value: T } | { type: 'error'; error: any }

export function catchPromise<T>(promise: Promise<T>): Promise<CatchedPromise<T>> {
  return promise
    .then<CatchedPromise<T>>(value => ({ type: 'success', value }))
    .catch(error => ({ type: 'error', error }))
}
