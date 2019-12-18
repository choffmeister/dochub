import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'

interface Props {
  seconds: number
}

export const Duration: React.FC<Props> = ({ seconds }) => {
  const styles = useStyles()
  return <span className={styles.root}>{formatSeconds(seconds)}</span>
}

const useStyles = makeStyles(() => ({
  root: {
    whiteSpace: 'pre',
  },
}))

function formatSeconds(seconds: number): string {
  const s = Math.floor(seconds) % 60
  const m = Math.floor(seconds / 60) % 60
  const h = Math.floor(seconds / 60 / 60)
  if (h > 0) {
    return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  } else {
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }
}
