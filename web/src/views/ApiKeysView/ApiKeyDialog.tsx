import React from 'react'
import Button from '@material-ui/core/Button'
import TextField from '@material-ui/core/TextField'
import FormGroup from '@material-ui/core/FormGroup'
import { ApiKey, createApiKey, deleteApiKey } from '../../models/ApiKey'
import { useHttp } from '@airfocusio/react-http-provider'
import { Scope, scopes } from '../../models/Principal'
import { CheckboxList } from './CheckboxList'
import FormControl from '@material-ui/core/FormControl'
import makeStyles from '@material-ui/core/styles/makeStyles'
import DialogTitle from '@material-ui/core/DialogTitle'
import DialogContent from '@material-ui/core/DialogContent'
import DialogContentText from '@material-ui/core/DialogContentText'
import DialogActions from '@material-ui/core/DialogActions'
import Dialog from '@material-ui/core/Dialog'
import { Copyable } from '../../components/Copyable/Copyable'

interface Props {
  open: boolean
  onClose: (apiKey?: ApiKey) => void
}

interface Value {
  name: string
  scopes: Scope[]
}

const defaultValue: Value = { name: '', scopes: ['read'] }

export const ApiKeyDialog: React.FC<Props> = ({ open, onClose }) => {
  const { http } = useHttp()
  const styles = useStyles()
  const [pristine, setPristine] = React.useState(true)
  const initialValue = React.useRef({ ...defaultValue })
  const [value, setValue] = React.useState<Value>(initialValue.current)
  const [created, setCreated] = React.useState<ApiKey | undefined>(undefined)
  React.useEffect(() => {
    if (open) {
      setPristine(true)
      setValue(initialValue.current)
      setCreated(undefined)
    }
  }, [open])
  React.useEffect(() => {
    if (value !== initialValue.current) {
      setPristine(false)
    }
  }, [value])

  return (
    <Dialog open={open} onClose={() => onClose(created)} aria-labelledby="form-dialog-title">
      <form
        onSubmit={async (event) => {
          event.preventDefault()
          const apiKey = await createApiKey(http, value.name, value.scopes)
          setCreated(apiKey)
        }}>
        <DialogTitle id="form-dialog-title">Create a new API key</DialogTitle>
        <DialogContent>
          <DialogContentText>
            You can use API keys to securely authenticate against the registries when pulling or pushing packages.
          </DialogContentText>
          <FormControl component="fieldset" className={styles.root}>
            <FormGroup className={styles.formGroup}>
              <TextField
                label="Name"
                value={value.name}
                onChange={(event) => setValue({ ...value, name: event.target.value })}
                required
                onBlur={() => setPristine(false)}
                error={!pristine && !value.name.trim()}
                autoFocus
                disabled={!!created}
              />
            </FormGroup>
          </FormControl>
          <DialogContentText>
            By default an API key grants the same permissions as you have. Often it is a good idea to narrow down the
            permissions so that the API key only grants the permissions needed for its use case.
          </DialogContentText>
          <FormControl component="fieldset" className={styles.root}>
            <FormGroup className={styles.formGroup}>
              <CheckboxList
                label="Scopes"
                options={scopes}
                value={value.scopes}
                setValue={(scopes) => setValue({ ...value, scopes })}
                horizontal
                required
                error={value.scopes.length === 0}
                disabled={!!created}
              />
            </FormGroup>
          </FormControl>
          {!!created && (
            <div className={styles.secretRoot}>
              <Copyable value={created.secret}>
                {(copy) => (
                  <div onClick={copy} className={styles.secret}>
                    {created.secret}
                  </div>
                )}
              </Copyable>
              <div className={styles.secretMessage}>
                This is your API key secret. Use it as password/token together with the username "api" to authenticate
                against the registries. This secret is only shown once and will never be shown again. Please write it
                done somewhere self. If you loose the secret you need to create a new API key.
              </div>
            </div>
          )}
        </DialogContent>
        <DialogActions>
          {!created && (
            <Button onClick={() => onClose(undefined)} color="primary">
              Cancel
            </Button>
          )}
          {!created && (
            <Button color="primary" disabled={!value.name.trim() || value.scopes.length === 0} type="submit">
              Create
            </Button>
          )}
          {!!created && (
            <Button
              onClick={async () => {
                await deleteApiKey(http, created.id)
                onClose(undefined)
              }}>
              Undo
            </Button>
          )}
          {!!created && (
            <Button color="primary" onClick={() => onClose(created)}>
              Close
            </Button>
          )}
        </DialogActions>
      </form>
    </Dialog>
  )
}

const useStyles = makeStyles((theme) => ({
  root: {
    width: '100%',
  },
  formGroup: {
    margin: '20px 0',
  },
  secretRoot: {
    padding: theme.spacing(3),
    border: '1px solid #ffa000',
    borderRadius: 8,
    backgroundColor: '#ffa00030',
    textAlign: 'center',
  },
  secret: {
    fontFamily: 'SFMono-Regular, Consolas, Liberation Mono, Menlo, monospace',
    fontSize: '170%',
    width: '100%',
    background: '0',
    border: '0',
    outline: '0',
    textAlign: 'center',
    textDecoration: 'underline',
    resize: 'none',
  },
  secretMessage: {
    paddingTop: theme.spacing(2),
  },
}))
