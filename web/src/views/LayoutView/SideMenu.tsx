import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import { useAuth } from '@airfocusio/react-auth-provider'
import { useHistory } from 'react-router'
import { Principal } from '../../models/Principal'
import Drawer from '@material-ui/core/Drawer'
import List from '@material-ui/core/List'
import ListItem from '@material-ui/core/ListItem'
import ListItemText from '@material-ui/core/ListItemText'
import Divider from '@material-ui/core/Divider'
import { apiKeysUrl, homeUrl } from './urls'

interface Props {
  open: boolean
  onChangeOpen: (open: boolean) => void
}

export const SideMenu: React.FC<Props> = ({ open, onChangeOpen }) => {
  const history = useHistory()
  const styles = useStyles()
  const { credentials, logout } = useAuth<Principal>()
  return (
    <Drawer open={open} onClose={() => onChangeOpen(false)}>
      <div
        className={styles.root}
        role="presentation"
        onClick={() => onChangeOpen(false)}
        onKeyDown={() => onChangeOpen(false)}>
        <List>
          <ListItem button onClick={() => history.push(homeUrl)}>
            <ListItemText primary="Home" />
          </ListItem>
          {!!credentials && (
            <React.Fragment>
              <Divider />
              <ListItem button onClick={() => history.push(apiKeysUrl)}>
                <ListItemText primary="API keys" />
              </ListItem>
            </React.Fragment>
          )}
          <Divider />
          {!credentials ? (
            <ListItem button onClick={() => (window.location.href = '/api/auth/github')}>
              <ListItemText primary="Login" />
            </ListItem>
          ) : (
            <ListItem
              button
              onClick={() => {
                logout()
                history.push(homeUrl)
              }}>
              <ListItemText primary="Logout" />
            </ListItem>
          )}
        </List>
      </div>
    </Drawer>
  )
}

const useStyles = makeStyles(theme => ({
  root: {
    width: 250,
  },
}))
