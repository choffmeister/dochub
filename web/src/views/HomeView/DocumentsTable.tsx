import React from 'react'
import TableCell from '@material-ui/core/TableCell'
import Chip from '@material-ui/core/Chip'
import { DefaultTable } from '../../components/DefaultTable/DefaultTable'
import { Document, listDocuments } from '../../models/Document'
import { AxiosInstance } from 'axios'
import { Timestamp } from '../../components/Timestamp/Timestamp'
import { ContentTypeLogo } from '../../components/ContentTypeLogo/ContentTypeLogo'
import { formatFileSize } from '../../utils/formatFileSize'

interface Props {
  onClickDocument?: (document: Document) => void
}

export const DocumentsTable: React.FC<Props> = ({ onClickDocument }) => {
  const customListDocuments = React.useMemo(
    () => (http: AxiosInstance, from: number, limit: number) => listDocuments(http, from, limit),
    []
  )

  return (
    <DefaultTable
      title="Documents"
      data={customListDocuments}
      header={
        <React.Fragment>
          <TableCell>Name</TableCell>
          <TableCell align="right">Revision</TableCell>
          <TableCell align="right">Size</TableCell>
          <TableCell align="right">Created at</TableCell>
        </React.Fragment>
      }
      row={(document, page) => (
        <React.Fragment>
          <TableCell component="th" scope="row" onClick={() => onClickDocument && onClickDocument(document)}>
            <div>
              <ContentTypeLogo contentType={document.contentType} />
              {document.name}
            </div>
            <div>
              {document.labelIds.map((labelId) => (
                <Chip
                  key={labelId}
                  label={page._embedded.labels[labelId] ? page._embedded.labels[labelId]!.name : '???'}
                />
              ))}
            </div>
          </TableCell>
          <TableCell align="right">{document.revisionNumber}</TableCell>
          <TableCell align="right">{formatFileSize(document.size)}</TableCell>
          <TableCell align="right">
            {document.createdAt ? <Timestamp timestamp={document.createdAt} /> : null}
          </TableCell>
        </React.Fragment>
      )}
      emptyRow={
        <TableCell component="th" scope="row" colSpan={4}>
          There are no documents that your are allowed to see.
        </TableCell>
      }
    />
  )
}
