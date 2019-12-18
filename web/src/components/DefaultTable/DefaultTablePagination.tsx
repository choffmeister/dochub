import React from 'react'
import TablePagination from '@material-ui/core/TablePagination'

interface Props {
  count: number
  page: number
  rowsPerPage: number
  setPage: (page: number) => void
  setRowsPerPage: (rowsPerPage: number) => void
}

export const DefaultTablePagination: React.FC<Props> = ({ count, page, rowsPerPage, setPage, setRowsPerPage }) => {
  return (
    <TablePagination
      rowsPerPageOptions={[5, 10, 25, 50, 100]}
      component="div"
      count={count}
      rowsPerPage={rowsPerPage}
      page={page}
      backIconButtonProps={{
        'aria-label': 'previous page',
      }}
      nextIconButtonProps={{
        'aria-label': 'next page',
      }}
      onChangePage={(_, page) => {
        setPage(page)
      }}
      onChangeRowsPerPage={event => {
        setRowsPerPage(parseInt(event.target.value, 10))
        setPage(0)
      }}
    />
  )
}
