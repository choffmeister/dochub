import * as React from 'react'

const baseTitle = document.title

export function useTitle(title?: String) {
  React.useEffect(() => {
    document.title = title ? `${title} - ${baseTitle}` : baseTitle
    return () => {
      document.title = baseTitle
    }
  }, [title])
}
