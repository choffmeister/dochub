import React from 'react'
import Button from '@material-ui/core/Button'
import MoreIcon from '@material-ui/icons/MoreVert'
import Popover from '@material-ui/core/Popover'
import makeStyles from '@material-ui/core/styles/makeStyles'

export const MoreMenu: React.FC = ({ children }) => {
  const anchor = React.useRef<HTMLButtonElement>(null)
  const [open, setOpen] = React.useState(false)
  const styles = useStyles()
  return (
    <React.Fragment>
      <Button
        ref={anchor}
        size="small"
        aria-label="more"
        onClick={() => setOpen(true)}
        variant="text"
        className={styles.button}>
        <MoreIcon />
      </Button>
      <Popover
        open={open}
        onClose={() => setOpen(false)}
        anchorEl={anchor.current}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}>
        {children}
      </Popover>
    </React.Fragment>
  )
}

const useStyles = makeStyles(() => ({
  button: {
    minWidth: 0,
    borderRadius: '50%',
    color: '#a9a9a9',
  },
}))
