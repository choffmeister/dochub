import React from 'react'
import MessageView from '../MessageView/MessageView'
import { useLocation } from 'react-router'
import { Location } from 'history'
import Button from '@material-ui/core/Button'
import { AxiosResponse } from 'axios'
import { homeUrl } from '../LayoutView/urls'

interface Props {
  error: any
  onRequestReset: () => void
}

const ErrorView: React.FC<Props> = ({ error, onRequestReset }) => {
  useLocationChanged(onRequestReset)
  const errorInfo = inspectError(error)
  return (
    <MessageView
      title={errorInfo.title}
      message={errorInfo.message}
      codeMessage={
        errorInfo.properties
          .filter(([, v]) => !!v)
          .map(([k, v]) => `${k}: ${v}`)
          .join('\n') || undefined
      }
      footer={
        !errorInfo.needsReload ? (
          <Button variant="contained" color="primary" onClick={onRequestReset}>
            Retry
          </Button>
        ) : (
          <Button variant="contained" color="primary" onClick={() => (window.location.href = homeUrl)}>
            Reload
          </Button>
        )
      }
    />
  )
}

export default ErrorView

type ErrorInfoProperties = Array<[string, any]>

interface ErrorInfo {
  title: string
  message: string
  properties: ErrorInfoProperties
  needsReload?: boolean
}

function inspectError(err: any): ErrorInfo {
  if (err && err.name === 'ChunkLoadError') {
    return {
      title: 'Network error',
      message: 'The server could not be reach due to an network error. Please try again.',
      properties: [],
      needsReload: true,
    }
  } else if (err && err.request && !err.response) {
    const requestProperties: ErrorInfoProperties = [
      [
        'requestMethod',
        err.config && typeof err.config.method === 'string' ? err.config.method.toUpperCase() : undefined,
      ],
      ['requestUrl', err.config && typeof err.config.url === 'string' ? err.config.url : undefined],
    ]
    return {
      title: 'Network error',
      message: 'The server could not be reach due to an network error. Please try again.',
      properties: requestProperties,
    }
  } else if (err && err.response && typeof err.response.status === 'number') {
    const requestResponseProperties: ErrorInfoProperties = [
      [
        'requestMethod',
        err.config && typeof err.config.method === 'string' ? err.config.method.toUpperCase() : undefined,
      ],
      ['requestUrl', err.config && typeof err.config.url === 'string' ? err.config.url : undefined],
      ['responseStatus', err.response.status],
      ['responseBody', stringifyResponseBody(err.response)],
    ]
    if (err.response.status === 401) {
      return {
        title: 'Unauthorized',
        message: 'You tried to access a resource without being authenticated. Please log in first.',
        properties: requestResponseProperties,
        needsReload: true,
      }
    } else if (err.response.status === 403) {
      return {
        title: 'Forbidden',
        message: 'You tried to access a resource that you have no access to.',
        properties: requestResponseProperties,
        needsReload: true,
      }
    } else if (err.response.status === 404) {
      return {
        title: 'Not found',
        message: 'You tried to access a resource that does not exist.',
        properties: requestResponseProperties,
        needsReload: true,
      }
    } else if ([502, 503, 504].indexOf(err.response.status) >= 0) {
      return {
        title: 'Service unavailable',
        message: 'The service is currently unavailable. Please try again later.',
        properties: requestResponseProperties,
      }
    } else {
      return {
        title: `Request error`,
        message: `A request failed with HTTP code ${err.response.status}.`,
        properties: requestResponseProperties,
        needsReload: true,
      }
    }
  } else {
    return {
      title: err && typeof err.message === 'string' ? err.message : 'Unknown error',
      message: 'An error has occured.',
      properties: ['stack', err.stack],
      needsReload: true,
    }
  }
}

function stringifyResponseBody(res: AxiosResponse): string {
  try {
    if (res && res.data) {
      if (typeof res.data === 'string') {
        return res.data
      } else if (typeof res.data === 'object') {
        return JSON.stringify(res.data, null, 2)
      } else {
        return ''
      }
    } else {
      return ''
    }
  } catch (err) {
    return ''
  }
}

function useLocationChanged(fn: (location: Location<any>) => void) {
  const location = useLocation()
  const lastLocation = React.useRef(location)
  React.useEffect(() => {
    if (location !== lastLocation.current) {
      fn(location)
      lastLocation.current = location
    }
  }, [location, lastLocation, fn])
}
