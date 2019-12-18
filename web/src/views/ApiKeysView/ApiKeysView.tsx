import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import Container from '@material-ui/core/Container'
import Paper from '@material-ui/core/Paper'
import { Navigation } from '../../components/Navigation/Navigation'
import { apiKeysUrl, homeUrl } from '../LayoutView/urls'
import { ApiKeysTable } from './ApiKeysTable'

const ApiKeysView: React.FC = () => {
  const styles = useStyles()
  return (
    <React.Fragment>
      <Container fixed>
        <Navigation
          entries={[
            { label: 'Home', to: homeUrl },
            { label: 'API keys', to: apiKeysUrl },
          ]}
        />
        <Paper className={styles.paper}>
          <ApiKeysTable />
        </Paper>
      </Container>
    </React.Fragment>
  )
}

export default ApiKeysView

const useStyles = makeStyles(() => ({
  paper: {
    width: '100%',
    overflowX: 'auto',
    margin: '20px 0',
  },
}))
