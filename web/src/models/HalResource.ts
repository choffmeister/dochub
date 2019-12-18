// TODO some ugly workaround for "spread types may only be created from object types"

export type HalResource<R, E> = R & {
  _embedded: E
}

export interface HalResourceEmbeddedMapper {
  [key: string]: (js: any) => any
}

export type HalResourceEmbeddedMapperResult<M extends HalResourceEmbeddedMapper> = { [K in keyof M]: ReturnType<M[K]> }

export function halResourceFromJSON<J, R, M extends HalResourceEmbeddedMapper>(
  js: J,
  resourceFromJSON: (js: J) => R,
  embeddedFromJSON: M
): HalResource<R, HalResourceEmbeddedMapperResult<M>> {
  const { _embedded, ...resource } = js as any
  return {
    ...(resourceFromJSON(resource) as any),
    _embedded: halResourceEmbeddedFromJSON(_embedded, embeddedFromJSON),
  }
}

export type HalPage<R, E> = HalResource<{ items: R[]; totalItems: number }, E>

export function halPageResourceFromJSON<J, R, M extends HalResourceEmbeddedMapper>(
  js: HalPage<J, any>,
  itemFromJSON: (js: J) => R,
  embeddedFromJSON: M
): HalResource<Pick<HalPage<R, any>, 'items' | 'totalItems'>, HalResourceEmbeddedMapperResult<M>> {
  return {
    items: js.items.map((js) => itemFromJSON(js)),
    totalItems: js.totalItems as number,
    _embedded: halResourceEmbeddedFromJSON(js._embedded, embeddedFromJSON),
  }
}

function halResourceEmbeddedFromJSON<M extends HalResourceEmbeddedMapper>(
  embedded: any,
  mapper: M
): HalResourceEmbeddedMapperResult<M> {
  return Object.keys(mapper).reduce(
    (acc, key) => ({
      ...acc,
      [key]: mapper[key]((embedded && embedded[key]) || {}),
    }),
    {} as any
  )
}

export function idLookupFromJSON<E>(entryFromJSON: (js: any) => E): (js: any) => { [id: string]: E | undefined } {
  return (js) =>
    Object.keys(js || {}).reduce(
      (acc, key) => ({
        ...acc,
        [key]: entryFromJSON(js[key]),
      }),
      {}
    )
}
