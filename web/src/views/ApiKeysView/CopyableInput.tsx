import React from 'react'
import Popper from '@material-ui/core/Popper'
import ClickAwayListener from '@material-ui/core/ClickAwayListener'
import Paper from '@material-ui/core/Paper'
import makeStyles from '@material-ui/core/styles/makeStyles'

type Props = React.DetailedHTMLProps<React.InputHTMLAttributes<HTMLInputElement>, HTMLInputElement>

export const CopyableInput: React.FC<Props> = ({ onClick, ...props }) => {
  const ref = React.useRef<HTMLInputElement>(null)
  const styles = useStyles()
  const [open, setOpen] = React.useState(false)
  const openTimeout = React.useRef(0)
  React.useEffect(() => {
    if (open) {
      window.clearTimeout(openTimeout.current)
      openTimeout.current = window.setTimeout(() => setOpen(false), 1500)
    }
    return () => window.clearTimeout(openTimeout.current)
  }, [open])

  return (
    <React.Fragment>
      <input
        ref={ref}
        {...props}
        onClick={event => {
          if (ref.current) {
            ref.current.select()
            setOpen(true)
          }
          if (onClick) {
            onClick(event)
          }
        }}
      />
      <Popper open={open} anchorEl={ref.current} placement="bottom" disablePortal>
        <ClickAwayListener onClickAway={() => setOpen(false)}>
          <Paper className={styles.paper} elevation={3}>
            Copied to clipboard
          </Paper>
        </ClickAwayListener>
      </Popper>
    </React.Fragment>
  )
}

const useStyles = makeStyles(theme => ({
  paper: {
    padding: theme.spacing(2),
  },
}))
