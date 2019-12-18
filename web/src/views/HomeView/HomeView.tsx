import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import Container from '@material-ui/core/Container'
import Paper from '@material-ui/core/Paper'
import { Navigation } from '../../components/Navigation/Navigation'
import { documentUrl, homeUrl } from '../LayoutView/urls'
import { DocumentsTable } from './DocumentsTable'
import { useHistory } from 'react-router'

const HomeView: React.FC = () => {
  const styles = useStyles()
  const history = useHistory()
  return (
    <Container fixed>
      <Navigation entries={[{ label: 'Home', to: homeUrl }]} />
      <Paper className={styles.paper}>
        <DocumentsTable onClickDocument={document => history.push(documentUrl(document.id))} />
      </Paper>
    </Container>
  )
}

export default HomeView

const useStyles = makeStyles(() => ({
  paper: {
    width: '100%',
    overflowX: 'auto',
    margin: '20px 0',
  },
}))
