import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import Button from '@material-ui/core/Button'
import queryString from 'query-string'

const LoginView: React.FC = () => {
  const styles = useStyles()
  const query = queryString.parse(window.location.search)
  const message = Array.isArray(query.message) ? query.message : [query.message]
  return (
    <div className={styles.root}>
      {!!message && (
        <div className={styles.messageContainer}>
          {message.map((line, idx) => (
            <div key={idx}>{line}</div>
          ))}
        </div>
      )}
      <div className={styles.buttonContainer}>
        <Button variant="contained" color="primary" onClick={() => (window.location.href = '/api/auth/github')}>
          Login with GitHub
        </Button>
      </div>
    </div>
  )
}

export default LoginView

const useStyles = makeStyles(theme => ({
  root: {
    width: '100vw',
    height: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'column',
  },
  messageContainer: {
    padding: theme.spacing(2),
  },
  buttonContainer: {
    padding: theme.spacing(2),
  },
}))
