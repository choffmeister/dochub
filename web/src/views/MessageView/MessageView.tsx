import React from 'react'
import Typography from '@material-ui/core/Typography'
import makeStyles from '@material-ui/core/styles/makeStyles'
import Container from '@material-ui/core/Container'
import Paper from '@material-ui/core/Paper'

interface Props {
  title: string
  message: string
  codeMessage?: string
  footer?: React.ReactNode
}

const MessageView: React.FC<Props> = ({ title, message, codeMessage, footer }) => {
  const styles = useStyles()
  const [showDetails, setShowDetails] = React.useState(false)
  return (
    <Container fixed>
      <Paper className={styles.paper}>
        <div className={styles.padding}>
          <Typography variant="h4">{title}</Typography>
          <Typography variant="body1" className={styles.message}>
            {message}
            {!!codeMessage && !showDetails && (
              <button onClick={() => setShowDetails(true)} className={styles.showDetails}>
                Show details
              </button>
            )}
          </Typography>
          {!!codeMessage && showDetails && <pre className={styles.codeMessage}>{codeMessage}</pre>}
          {!!footer && <div>{footer}</div>}
        </div>
      </Paper>
    </Container>
  )
}

export default MessageView

const useStyles = makeStyles((theme) => ({
  paper: {
    margin: '20px 0',
  },
  padding: {
    padding: theme.spacing(2),
  },
  message: {
    margin: '1em 0',
  },
  codeMessage: {
    fontFamily: 'SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace',
    overflow: 'auto',
    maxHeight: 200,
    padding: 10,
    backgroundColor: '#f7f7f7',
    border: '1px solid #e0e0e0',
    borderRadius: 3,
    margin: '1em 0',
  },
  showDetails: {
    outline: 0,
    border: 0,
    margin: '-3px 0 -3px 0.5em',
    padding: '3px 6px',
    borderRadius: 3,
    color: 'blue',
    textDecoration: 'underline',
    backgroundColor: '#e2e2e2',
    '&:hover': {
      backgroundColor: '#d6d6d6',
    },
    cursor: 'pointer',
  },
}))
