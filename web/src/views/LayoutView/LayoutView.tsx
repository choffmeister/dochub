import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import { Switch, Route, useHistory } from 'react-router'
import { ErrorBoundary } from '../../components/ErrorBoundary/ErrorBoundary'
import ErrorView from '../ErrorView/ErrorView'
import MessageView from '../MessageView/MessageView'
import { SideMenu } from './SideMenu'
import { TopBar } from './TopBar'
import Button from '@material-ui/core/Button'
import { homeUrl } from './urls'

const HomeView = React.lazy(() => import('../HomeView/HomeView'))
const ApiKeysView = React.lazy(() => import('../ApiKeysView/ApiKeysView'))
const DocumentView = React.lazy(() => import('../DocumentView/DocumentView'))

const LayoutView: React.FC = () => {
  const history = useHistory()
  const styles = useStyles()
  const [sideMenuOpen, setSideMenuOpen] = React.useState(false)
  return (
    <React.Fragment>
      <TopBar onMenuButtonClick={() => setSideMenuOpen(true)} />
      <div className={styles.content}>
        <ErrorBoundary renderError={(error, reset) => <ErrorView error={error} onRequestReset={reset} />}>
          <React.Suspense fallback={null}>
            <Switch>
              <Route path="/" exact render={() => <HomeView />} />
              <Route
                path="/document/:documentId"
                exact
                render={({ match }) => <DocumentView documentId={match.params.documentId} />}
              />
              <Route path="/settings/api-keys" exact render={() => <ApiKeysView />} />
              <Route
                render={() => (
                  <MessageView
                    title="Not found"
                    message="The requested page could not be found."
                    footer={
                      <Button variant="contained" color="primary" onClick={() => history.push(homeUrl)}>
                        Go back
                      </Button>
                    }
                  />
                )}
              />
            </Switch>
          </React.Suspense>
        </ErrorBoundary>
      </div>
      <SideMenu open={sideMenuOpen} onChangeOpen={setSideMenuOpen} />
    </React.Fragment>
  )
}

export default LayoutView

const useStyles = makeStyles(theme => ({
  content: {
    marginTop: theme.spacing(3),
  },
}))
