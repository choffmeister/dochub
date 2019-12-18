import React from 'react'
import makeStyles from '@material-ui/core/styles/makeStyles'
import InputBase from '@material-ui/core/InputBase'
import { fade } from '@material-ui/core/styles'
import SearchIcon from '@material-ui/icons/Search'
import Popper from '@material-ui/core/Popper'
import Paper from '@material-ui/core/Paper'
import { useHttpCall } from '@airfocusio/react-http-provider'
import Typography from '@material-ui/core/Typography'
import { catchPromise } from '../../utils/catchPromise'
import { AxiosInstance } from 'axios'
import { searchDocuments } from '../../models/Document'
import { documentUrl } from './urls'
import { useHistory } from 'react-router'

export const TopBarSearchInput: React.FC = () => {
  const styles = useStyles()
  const history = useHistory()
  const anchorRef = React.useRef<HTMLDivElement>(null)
  const inputRef = React.useRef<HTMLInputElement>(null)
  const [open, setOpen] = React.useState(false)
  const [text, setText] = React.useState('')
  const queryTimeout = React.useRef(0)
  const [query, setQuery] = React.useState('')
  const customSearch = React.useMemo(
    () => async (http: AxiosInstance, query: string) =>
      query ? catchPromise(searchDocuments(http, query, 0, 10)) : undefined,
    []
  )
  const resultPage = useHttpCall(customSearch, query)
  const [activeIndex, setActiveIndex] = React.useState(0)
  React.useEffect(() => setActiveIndex(0), [resultPage])
  return (
    <div ref={anchorRef} className={styles.root}>
      <div className={styles.icon}>
        <SearchIcon />
      </div>
      <InputBase
        value={text}
        onChange={(event) => {
          const text = event.target.value
          setText(text)
          window.clearTimeout(queryTimeout.current)
          queryTimeout.current = window.setTimeout(() => setQuery(text.trim()), 250)
        }}
        placeholder="Search…"
        classes={{
          root: styles.inputRoot,
          input: styles.inputInput,
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => {
          setOpen(false)
          setActiveIndex(0)
        }}
        onKeyDown={(event) => {
          switch (event.keyCode) {
            case 13:
              event.preventDefault()
              if (
                resultPage &&
                resultPage.type === 'success' &&
                activeIndex >= 0 &&
                activeIndex < resultPage.value.items.length
              ) {
                const result = resultPage.value.items[activeIndex]
                if (inputRef.current) {
                  inputRef.current.blur()
                }
                history.push(documentUrl(result.id))
              }
              break
            case 38:
              event.preventDefault()
              if (resultPage && resultPage.type === 'success' && resultPage.value.items.length > 0) {
                if (activeIndex >= 0 && activeIndex < resultPage.value.items.length) {
                  setActiveIndex(activeIndex > 0 ? activeIndex - 1 : resultPage.value.items.length - 1)
                } else {
                  setActiveIndex(resultPage.value.items.length - 1)
                }
              }
              break
            case 40:
              event.preventDefault()
              if (resultPage && resultPage.type === 'success' && resultPage.value.items.length > 0) {
                if (activeIndex >= 0 && activeIndex < resultPage.value.items.length) {
                  setActiveIndex(activeIndex < resultPage.value.items.length - 1 ? activeIndex + 1 : 0)
                } else {
                  setActiveIndex(0)
                }
              }
              break
            default:
              break
          }
        }}
        inputProps={{
          'aria-label': 'search',
          ref: inputRef,
        }}
      />
      <Popper open={open} anchorEl={anchorRef.current} placement="bottom-end" className={styles.suggestionsRoot}>
        {() => (
          <Paper
            className={styles.suggestionsContainer}
            onMouseDown={(event) => {
              event.preventDefault()
            }}>
            {!!resultPage &&
              resultPage.type === 'success' &&
              resultPage.value.items.length > 0 &&
              resultPage.value.items.map((result, index) => (
                <Typography
                  key={result.id}
                  className={activeIndex === index ? styles.suggestionContainerActive : styles.suggestionContainer}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => {
                    if (inputRef.current) {
                      inputRef.current.blur()
                    }
                    history.push(documentUrl(result.id))
                  }}
                  component="div">
                  {result.name}
                </Typography>
              ))}
            {!!resultPage && resultPage.type === 'error' && (
              <Typography className={styles.noSuggestionContainerContainer}>An error occured.</Typography>
            )}
            {(!resultPage || (resultPage.type === 'success' && resultPage.value.items.length === 0)) && (
              <Typography className={styles.noSuggestionContainerContainer}>No search results.</Typography>
            )}
          </Paper>
        )}
      </Popper>
    </div>
  )
}

const useStyles = makeStyles((theme) => ({
  root: {
    position: 'relative',
    borderRadius: theme.shape.borderRadius,
    backgroundColor: fade(theme.palette.common.white, 0.15),
    '&:hover': {
      backgroundColor: fade(theme.palette.common.white, 0.25),
    },
    marginRight: theme.spacing(2),
    marginLeft: 0,
    width: '100%',
    [theme.breakpoints.up('sm')]: {
      marginLeft: theme.spacing(3),
      width: 'auto',
    },
  },
  icon: {
    width: theme.spacing(7),
    height: '100%',
    position: 'absolute',
    pointerEvents: 'none',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  inputRoot: {
    color: 'inherit',
  },
  inputInput: {
    padding: theme.spacing(1, 1, 1, 7),
    transition: theme.transitions.create('width'),
    width: '100%',
    [theme.breakpoints.up('md')]: {
      width: 200,
    },
  },
  suggestionsRoot: {
    zIndex: theme.zIndex.appBar + 1,
  },
  suggestionsContainer: {
    width: 300,
  },
  noSuggestionContainerContainer: {
    padding: theme.spacing(2),
  },
  suggestionContainer: {
    cursor: 'pointer',
    padding: theme.spacing(2),
  },
  suggestionContainerActive: {
    cursor: 'pointer',
    padding: theme.spacing(2),
    backgroundColor: '#f0f0f0',
  },
}))
