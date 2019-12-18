import React from 'react'
import ClickAwayListener from '@material-ui/core/ClickAwayListener'
import Tooltip from '@material-ui/core/Tooltip'

interface Props {
  value: string
  children: (copy: () => void) => React.ReactElement
}

export const Copyable: React.FC<Props> = ({ value, children }) => {
  const ref = React.useRef<HTMLElement>()
  const [tooltipOpen, setTooltipOpen] = React.useState(false)
  const copy = React.useMemo(
    () => () => {
      if (ref.current && ref.current.parentElement) {
        stringToClipboard(value, ref.current.parentElement)
        setTooltipOpen(true)
      }
    },
    [value, setTooltipOpen, ref]
  )
  return (
    <ClickAwayListener onClickAway={() => setTooltipOpen(false)}>
      <Tooltip
        ref={ref}
        PopperProps={{ placement: 'bottom' }}
        onClose={() => setTooltipOpen(false)}
        open={tooltipOpen}
        disableFocusListener
        disableHoverListener
        disableTouchListener
        title="Copied to clipboard">
        {children(copy)}
      </Tooltip>
    </ClickAwayListener>
  )
}

function stringToClipboard(str: string, container: HTMLElement): void {
  const textarea = document.createElement('textarea')
  console.log(str)
  textarea.innerText = str
  textarea.style.position = 'fixed'
  textarea.style.top = '0px'
  textarea.style.left = '-1000px'
  textarea.style.width = '500px'
  textarea.style.zIndex = '999999'
  container.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  container.removeChild(textarea)
}
