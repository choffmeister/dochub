import React from 'react'
import { useHistory } from 'react-router'
import Breadcrumbs from '@material-ui/core/Breadcrumbs'
import Link from '@material-ui/core/Link'
import makeStyles from '@material-ui/core/styles/makeStyles'
import { useTitle } from '../useTitle'

interface Props {
  entries: Array<{ label: string; to: string }>
}

export const Navigation: React.FC<Props> = ({ entries }) => {
  const history = useHistory()
  const styles = useStyles()
  const init = entries.slice(0, entries.length - 1)
  const last = entries[entries.length - 1]
  useTitle(last.label)
  return (
    <Breadcrumbs aria-label="breadcrumb">
      {init.map((entry, index) => (
        <Link key={index} color="inherit" onClick={() => history.push(entry.to)} className={styles.segment}>
          {entry.label}
        </Link>
      ))}
      <Link color="textPrimary" onClick={() => history.push(last.to)} className={styles.segment}>
        {last.label}
      </Link>
    </Breadcrumbs>
  )
}

const useStyles = makeStyles(() => ({
  segment: {
    wordBreak: 'break-word',
  },
}))
