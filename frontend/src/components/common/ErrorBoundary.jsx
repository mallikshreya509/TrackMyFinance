import { Component } from 'react'

export default class ErrorBoundary extends Component {
  state = { error: null }

  static getDerivedStateFromError(error) {
    return { error }
  }

  render() {
    if (this.state.error) {
      return (
        <div className="page">
          <h1>Something went wrong</h1>
          <pre className="error">{String(this.state.error.message || this.state.error)}</pre>
          <button onClick={() => window.location.assign('/')}>Back to dashboard</button>
        </div>
      )
    }
    return this.props.children
  }
}