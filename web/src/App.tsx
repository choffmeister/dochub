import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import { useAuth } from '@airfocusio/react-auth-provider'
import { Principal } from './models/Principal'
import LayoutView from './views/LayoutView/LayoutView'
import LoginView from './views/LoginView/LoginView'
import CssBaseline from '@material-ui/core/CssBaseline'
import { useHttp } from '@airfocusio/react-http-provider'
import { createDocument } from './models/Document'
import { useGlobalFileDrop } from './components/useGlobalFileDrop'

export const App: React.FC = () => {
  useStyles()
  const { http, globalRefresh } = useHttp()
  const { credentials } = useAuth<Principal>()

  useGlobalFileDrop(async file => {
    await createDocument(http, file.name, file.type || 'application/octet-stream', file)
    await globalRefresh()
  })

  return (
    <React.Fragment>
      <CssBaseline />
      {!!credentials ? <LayoutView /> : <LoginView />}
    </React.Fragment>
  )
}

const useStyles = makeStyles(theme => ({
  '@global': {
    html: {
      margin: 0,
      padding: 0,
    },
    body: {
      margin: 0,
      padding: 0,
    },
  },
}))
