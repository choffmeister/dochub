import * as React from 'react'

export interface ErrorBoundaryProps {
  renderError: (err: any, reset: () => void) => React.ReactNode
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps> {
  public state = {
    error: null,
  }

  public componentDidCatch(error: Error) {
    // tslint:disable-next-line no-console
    console.error(error)
    this.setState({ error })
  }

  public render() {
    const { renderError, children } = this.props
    const { error } = this.state
    return error ? renderError(error, () => this.setState({ error: null })) : children
  }
}
