import React from 'react'
import ReactDOM from 'react-dom'
import { App } from './App'
import * as serviceWorker from './serviceWorker'
import axios from 'axios'
import queryString from 'query-string'
import { authorizeRefresh, AuthProvider } from '@airfocusio/react-auth-provider'
import { HttpProvider } from '@airfocusio/react-http-provider'
import { createBrowserHistory } from 'history'
import { Router } from 'react-router-dom'
import { principalFromJSON } from './models/Principal'

const Index: React.FC = () => {
  const http = React.useRef(
    axios.create({
      paramsSerializer: queryString.stringify,
    })
  )
  const authUrl = '/api/auth/token/create'
  return (
    <AuthProvider
      http={http.current}
      parseTokenData={principalFromJSON}
      url={authUrl}
      customInitialize={async () => {
        const query = queryString.parse(window.location.search)
        const explicitRefreshToken = Array.isArray(query.refreshToken) ? query.refreshToken[0] : query.refreshToken
        if (explicitRefreshToken) {
          return authorizeRefresh(http.current, authUrl, explicitRefreshToken)
        } else {
          return undefined
        }
      }}>
      <HttpProvider http={http.current}>
        <Router history={createBrowserHistory()}>
          <App />
        </Router>
      </HttpProvider>
    </AuthProvider>
  )
}

ReactDOM.render(<Index />, document.getElementById('root'))

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister()
