import React from 'react'
import ReactDOM from 'react-dom'
import { App } from './App'
import * as serviceWorker from './serviceWorker'
import axios from 'axios'
import queryString from 'query-string'
import { HttpProvider } from '@airfocusio/react-http-provider'
import { createBrowserHistory } from 'history'
import { Router } from 'react-router-dom'

const Index: React.FC = () => {
  const http = React.useRef(
    axios.create({
      paramsSerializer: queryString.stringify,
    })
  )
  return (
    <HttpProvider http={http.current}>
      <Router history={createBrowserHistory()}>
        <App />
      </Router>
    </HttpProvider>
  )
}

ReactDOM.render(<Index />, document.getElementById('root'))

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister()
