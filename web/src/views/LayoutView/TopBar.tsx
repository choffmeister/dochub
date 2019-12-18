import React from 'react'
import Typography from '@material-ui/core/Typography'
import IconButton from '@material-ui/core/IconButton'
import Toolbar from '@material-ui/core/Toolbar'
import AppBar from '@material-ui/core/AppBar'
import makeStyles from '@material-ui/core/styles/makeStyles'
import MenuIcon from '@material-ui/icons/Menu'
import { useHistory } from 'react-router'
import { TopBarSearchInput } from './TopBarSearchInput'
import { homeUrl } from './urls'

interface Props {
  onMenuButtonClick: () => void
}

export const TopBar: React.FC<Props> = ({ onMenuButtonClick }) => {
  const history = useHistory()
  const styles = useStyles()
  return (
    <AppBar position="sticky">
      <Toolbar>
        <IconButton
          edge="start"
          className={styles.menuButton}
          color="inherit"
          aria-label="menu"
          onClick={() => onMenuButtonClick()}>
          <MenuIcon />
        </IconButton>
        <Typography variant="h6" className={styles.title} onClick={() => history.push(homeUrl)}>
          dochub
        </Typography>
        <TopBarSearchInput />
      </Toolbar>
    </AppBar>
  )
}

const useStyles = makeStyles((theme) => ({
  menuButton: {
    marginRight: theme.spacing(2),
  },
  title: {
    flexGrow: 1,
    [theme.breakpoints.down('xs')]: {
      display: 'none',
    },
  },
}))
