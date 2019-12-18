import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import dayjs from 'dayjs'
import { Tooltip } from '@material-ui/core'

interface Props {
  timestamp: dayjs.Dayjs
}

export const Timestamp: React.FC<Props> = ({ timestamp }) => {
  const styles = useStyles()
  const [text, setText] = React.useState(formatTimePassed(timestamp))
  React.useEffect(() => {
    const intervalId = window.setInterval(() => {
      setText(formatTimePassed(timestamp))
    }, 10000)
    return () => window.clearInterval(intervalId)
  })
  return (
    <Tooltip title={timestamp.format('D MMM YYYY, HH:mm:ss')}>
      <span className={styles.root}>{text}</span>
    </Tooltip>
  )
}

const useStyles = makeStyles(() => ({
  root: {
    whiteSpace: 'pre',
  },
}))

function formatTimePassed(timestamp: dayjs.Dayjs): string {
  const seconds = dayjs().diff(timestamp, 'second')
  if (seconds < 60) {
    return 'just now'
  } else if (seconds < 60 * 2) {
    return 'one minute ago'
  } else if (seconds < 60 * 60) {
    return `${Math.floor(seconds / 60)} minutes ago`
  } else if (seconds < 60 * 60 * 2) {
    return 'one hour ago'
  } else if (seconds < 60 * 60 * 24) {
    return `${Math.floor(seconds / 60 / 60)} hours ago`
  } else if (seconds < 60 * 60 * 24 * 2) {
    return 'one day ago'
  } else if (seconds < 60 * 60 * 24 * 30) {
    return `${Math.floor(seconds / 60 / 60 / 24)} days ago`
  } else {
    return timestamp.format('D MMM YYYY, HH:mm')
  }
}
