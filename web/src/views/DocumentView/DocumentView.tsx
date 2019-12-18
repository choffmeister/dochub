import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import Container from '@material-ui/core/Container'
import Paper from '@material-ui/core/Paper'
import { Navigation } from '../../components/Navigation/Navigation'
import { documentUrl, homeUrl } from '../LayoutView/urls'
import { useHttp, useHttpCall } from '@airfocusio/react-http-provider'
import { retrieveDocument } from '../../models/Document'
import { useAuth } from '@airfocusio/react-auth-provider'

interface Props {
  documentId: string
}

const DocumentView: React.FC<Props> = ({ documentId }) => {
  const styles = useStyles()
  const { http, globalRefresh } = useHttp()
  const { credentials } = useAuth()
  const document = useHttpCall(retrieveDocument, documentId)
  const previewSrc = React.useMemo(
    () =>
      document && credentials
        ? `/api/documents/${document.id}/${document.revisionNumber}/download?token=${credentials.tokens.access}#toolbar=0&navpanes=0`
        : undefined,
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [document, !!credentials]
  )
  const downloadSrc = React.useMemo(
    () =>
      document && credentials
        ? `/api/documents/${document.id}/${document.revisionNumber}/download?token=${credentials.tokens.access}`
        : undefined,
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [document, !!credentials && credentials.tokens.access]
  )
  return document ? (
    <Container fixed>
      <Navigation
        entries={[
          { label: 'Home', to: homeUrl },
          { label: document.name, to: documentUrl(documentId) },
        ]}
      />
      <div>
        <div>
          <a href={downloadSrc} download>
            Download
          </a>
        </div>
        <div>
          <a
            href={documentUrl(documentId)}
            onClick={async (event) => {
              event.preventDefault()
              await http.post(`/api/documents/${documentId}/ocr`)
              await globalRefresh()
            }}>
            OCR
          </a>
        </div>
      </div>
      <Paper className={styles.paper}>
        {document.contentType === 'application/pdf' && (
          <div className={styles.iframeContainer}>
            <iframe title={document.name} src={previewSrc} className={styles.iframe} />
          </div>
        )}
      </Paper>
    </Container>
  ) : null
}

export default DocumentView

const useStyles = makeStyles(() => ({
  paper: {
    width: '100%',
    overflowX: 'auto',
    margin: '20px 0',
  },
  iframeContainer: {
    position: 'relative',
    paddingBottom: '75%',
  },
  iframe: {
    position: 'absolute',
    width: '100%',
    height: '100%',
    display: 'block',
    border: 0,
    margin: 0,
    padding: 0,
  },
}))
