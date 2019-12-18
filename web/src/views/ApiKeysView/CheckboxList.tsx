import React from 'react'
import FormControlLabel from '@material-ui/core/FormControlLabel'
import Checkbox from '@material-ui/core/Checkbox'
import FormControl from '@material-ui/core/FormControl'
import FormLabel from '@material-ui/core/FormLabel'
import FormGroup from '@material-ui/core/FormGroup'
import makeStyles from '@material-ui/core/styles/makeStyles'

interface Props<T> {
  label: string
  options: T[]
  value: T[]
  setValue: (value: T[]) => void
  horizontal?: boolean
  required?: boolean
  error?: boolean
  disabled?: boolean
  optionDisabled?: (value: T) => boolean
}

export function CheckboxList<T>(props: Props<T>) {
  const { label, options, value, setValue, horizontal, required, error, disabled, optionDisabled } = props
  const styles = useStyles()
  return (
    <FormControl component="fieldset" required={required} error={error}>
      <FormLabel component="legend">{label}</FormLabel>
      <FormGroup className={horizontal ? styles.horizontal : undefined}>
        {options.map((option, index) => (
          <FormControlLabel
            key={index}
            control={
              <Checkbox
                color="primary"
                checked={value.indexOf(option) >= 0}
                onChange={event => {
                  if (event.target.checked) {
                    setValue(value.concat([option]))
                  } else {
                    setValue(value.filter(s => s !== option))
                  }
                }}
                inputProps={{
                  'aria-label': 'checkbox',
                }}
                disabled={disabled || (optionDisabled && optionDisabled(option))}
              />
            }
            label={option}
          />
        ))}
      </FormGroup>
    </FormControl>
  )
}

const useStyles = makeStyles(() => ({
  horizontal: {
    flexDirection: 'row',
  },
}))
