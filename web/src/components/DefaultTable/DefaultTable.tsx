import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import { useDynamicHttpCall } from '@airfocusio/react-http-provider'
import Table from '@material-ui/core/Table'
import TableBody from '@material-ui/core/TableBody'
import TableCell from '@material-ui/core/TableCell'
import TableHead from '@material-ui/core/TableHead'
import TableRow from '@material-ui/core/TableRow'
import { DefaultTablePagination } from './DefaultTablePagination'
import Checkbox from '@material-ui/core/Checkbox'
import Toolbar from '@material-ui/core/Toolbar'
import Typography from '@material-ui/core/Typography'
import { HalPage } from '../../models/HalResource'
import { AxiosInstance } from 'axios'

interface Props<R extends { id: string }, E> {
  title: string
  data: (http: AxiosInstance, pageIndex: number, rowsPerPage: number) => Promise<HalPage<R, E>>
  actions?: React.ReactNode
  bulkActions?: (selected: string[], unselect: () => void, page: HalPage<R, E>) => React.ReactNode
  header: React.ReactNode
  row: (item: R, page: HalPage<R, E>) => React.ReactNode
  emptyRow: React.ReactNode
}

export function DefaultTable<R extends { id: string }, E>({
  title,
  data,
  actions,
  bulkActions,
  header,
  row,
  emptyRow,
}: Props<R, E>) {
  const styles = useStyles()
  const [pageIndex, setPageIndex] = React.useState(0)
  const [rowsPerPage, setRowsPerPage] = React.useState(25)
  const page = useDynamicHttpCall(data, pageIndex * rowsPerPage, rowsPerPage)
  const [selected, setSelected] = React.useState<string[]>([])
  return page ? (
    <React.Fragment>
      <Toolbar className={styles.toolbar}>
        {selected.length === 0 ? (
          <React.Fragment>
            <Typography variant="h6" className={styles.toolbarTitle}>
              {title}
            </Typography>
            {!!actions && actions}
          </React.Fragment>
        ) : (
          <React.Fragment>
            <Typography variant="subtitle1" className={styles.toolbarTitle}>
              {`${selected.length} selected`}
            </Typography>
            {!!bulkActions && bulkActions(selected, () => setSelected([]), page)}
          </React.Fragment>
        )}
      </Toolbar>
      <Table className={styles.table}>
        <TableHead>
          <TableRow>
            {!!bulkActions && page.items.length > 0 && (
              <TableCell padding="checkbox">
                <Checkbox
                  checked={selected.length > 0 && page.items.every(item => selected.indexOf(item.id) >= 0)}
                  disabled={page.items.length === 0}
                  indeterminate={selected.length > 0 && page.items.some(item => selected.indexOf(item.id) < 0)}
                  onChange={event => setSelected(event.target.checked ? page.items.map(item => item.id) : [])}
                />
              </TableCell>
            )}
            {header}
          </TableRow>
        </TableHead>
        <TableBody>
          {page.items.map(item => (
            <TableRow key={item.id}>
              {!!bulkActions && (
                <TableCell padding="checkbox">
                  <Checkbox
                    checked={selected.indexOf(item.id) >= 0}
                    onChange={event =>
                      setSelected(selected.filter(id => id !== item.id).concat(event.target.checked ? [item.id] : []))
                    }
                  />
                </TableCell>
              )}
              {row(item, page)}
            </TableRow>
          ))}
          {page.items.length === 0 && <TableRow>{emptyRow}</TableRow>}
        </TableBody>
      </Table>
      <DefaultTablePagination
        count={page.totalItems}
        page={pageIndex}
        rowsPerPage={rowsPerPage}
        setPage={setPageIndex}
        setRowsPerPage={setRowsPerPage}
      />
    </React.Fragment>
  ) : null
}

const useStyles = makeStyles(theme => ({
  toolbar: {
    paddingLeft: theme.spacing(2),
    paddingRight: theme.spacing(2),
  },
  toolbarTitle: {
    flex: '1 1 100%',
  },
  table: {
    minWidth: 350,
  },
}))
