import React from 'react'
import { useHttp } from '@airfocusio/react-http-provider'
import { ApiKey, deleteApiKey, listApiKeys } from '../../models/ApiKey'
import TableCell from '@material-ui/core/TableCell'
import { ConfirmedButton } from '../../components/ConfirmedButton/ConfirmedButton'
import Button from '@material-ui/core/Button'
import { ApiKeyDialog } from './ApiKeyDialog'
import { DefaultTable } from '../../components/DefaultTable/DefaultTable'
import { Timestamp } from '../../components/Timestamp/Timestamp'

interface Props {
  onClickApiKey?: (apiKey: ApiKey) => void
}

export const ApiKeysTable: React.FC<Props> = ({ onClickApiKey }) => {
  const { http, globalRefresh } = useHttp()
  const [dialogOpen, setDialogOpen] = React.useState(false)

  return (
    <React.Fragment>
      <DefaultTable
        title="API keys"
        data={listApiKeys}
        actions={
          <React.Fragment>
            <Button color="primary" onClick={() => setDialogOpen(true)}>
              Create
            </Button>
          </React.Fragment>
        }
        bulkActions={(selectedIds, unselect) => (
          <React.Fragment>
            <ConfirmedButton
              color="secondary"
              confirmationMessage="Sure you want to delete the selected API keys? This cannot be undone."
              confirmationButtonLabel="Delete"
              onConfirmed={async () => {
                await selectedIds.reduce((acc, id) => acc.then(() => deleteApiKey(http, id)), Promise.resolve())
                unselect()
                await globalRefresh()
              }}>
              Delete
            </ConfirmedButton>
          </React.Fragment>
        )}
        header={
          <React.Fragment>
            <TableCell>Name</TableCell>
            <TableCell align="right">Scopes</TableCell>
            <TableCell align="right">Last used at</TableCell>
          </React.Fragment>
        }
        row={(ak, page) => (
          <React.Fragment>
            <TableCell component="th" scope="row" onClick={() => onClickApiKey && onClickApiKey(ak)}>
              {ak.name}
            </TableCell>
            <TableCell align="right">{(ak.scopes || []).join(', ')}</TableCell>
            <TableCell align="right">{ak.lastUsedAt ? <Timestamp timestamp={ak.lastUsedAt} /> : '-'}</TableCell>
          </React.Fragment>
        )}
        emptyRow={
          <TableCell component="th" scope="row" colSpan={3}>
            You don't have any API keys yet.
          </TableCell>
        }
      />
      <ApiKeyDialog
        open={dialogOpen}
        onClose={async apiKey => {
          setDialogOpen(false)
          if (apiKey) {
            await globalRefresh()
          }
        }}
      />
    </React.Fragment>
  )
}
