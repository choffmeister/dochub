import React from 'react'
import { App } from './App'
import { createMemoryHistory } from 'history'
import { Router } from 'react-router'
import { render } from '@testing-library/react'

it('renders without crashing', () => {
  const history = createMemoryHistory()
  const dom = render(
    <Router history={history}>
      <App />
    </Router>
  )
  expect(dom.queryByText('dochub')).toBeDefined()
})
