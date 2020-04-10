import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import { useHistory } from 'react-router'
import Drawer from '@material-ui/core/Drawer'
import List from '@material-ui/core/List'
import ListItem from '@material-ui/core/ListItem'
import ListItemText from '@material-ui/core/ListItemText'
import { homeUrl } from './urls'

interface Props {
  open: boolean
  onChangeOpen: (open: boolean) => void
}

export const SideMenu: React.FC<Props> = ({ open, onChangeOpen }) => {
  const history = useHistory()
  const styles = useStyles()
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
        </List>
      </div>
    </Drawer>
  )
}

const useStyles = makeStyles((theme) => ({
  root: {
    width: 250,
  },
}))
