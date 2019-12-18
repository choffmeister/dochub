import React from 'react'
import Popper from '@material-ui/core/Popper'
import ClickAwayListener from '@material-ui/core/ClickAwayListener'
import Button from '@material-ui/core/Button'
import Paper from '@material-ui/core/Paper'
import makeStyles from '@material-ui/core/styles/makeStyles'
import Typography from '@material-ui/core/Typography'
import { PropTypes } from '@material-ui/core'

interface Props {
  color?: PropTypes.Color
  disabled?: boolean
  confirmationMessage: string
  confirmationButtonLabel: string
  onConfirmed: () => void
}

export const ConfirmedButton: React.FC<Props> = ({
  color,
  disabled,
  confirmationMessage,
  confirmationButtonLabel,
  onConfirmed,
  children,
}) => {
  const anchorRef = React.useRef<HTMLButtonElement>(null)
  const [open, setOpen] = React.useState(false)
  const styles = useStyles()
  return (
    <React.Fragment>
      <Button ref={anchorRef} color={color} onClick={() => !disabled && setOpen(true)} disabled={disabled}>
        {children}
      </Button>
      <Popper open={open} anchorEl={anchorRef.current} placement="bottom-end">
        <ClickAwayListener onClickAway={() => setOpen(false)}>
          <Paper className={styles.confirmationPaper} elevation={3}>
            <Typography>{confirmationMessage}</Typography>
            <Button
              variant="contained"
              color={color}
              onClick={() => {
                setOpen(false)
                onConfirmed()
              }}
              className={styles.confirmationButton}>
              {confirmationButtonLabel}
            </Button>
          </Paper>
        </ClickAwayListener>
      </Popper>
    </React.Fragment>
  )
}

const useStyles = makeStyles(theme => ({
  confirmationPaper: {
    padding: theme.spacing(2),
    display: 'grid',
    gridGap: theme.spacing(2),
    width: 250,
  },
  confirmationButton: {
    width: '100%,',
  },
}))
