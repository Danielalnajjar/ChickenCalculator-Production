import React, { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null
    };
  }

  static getDerivedStateFromError(error: Error): State {
    return {
      hasError: true,
      error,
      errorInfo: null
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
    
    // Log to external service in production
    if (process.env.NODE_ENV === 'production') {
      // TODO: Send to error tracking service like Sentry
      this.logErrorToService(error, errorInfo);
    }
    
    this.setState({
      error,
      errorInfo
    });
  }

  logErrorToService = (error: Error, errorInfo: ErrorInfo) => {
    // Placeholder for error logging service
    // In production, this would send to Sentry, LogRocket, etc.
    const errorData = {
      message: error.toString(),
      stack: error.stack,
      componentStack: errorInfo.componentStack,
      timestamp: new Date().toISOString(),
      userAgent: navigator.userAgent,
      url: window.location.href
    };
    
    // Send to logging endpoint
    fetch('/api/errors', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(errorData)
    }).catch(err => {
      console.error('Failed to log error to service:', err);
    });
  };

  handleReset = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null
    });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return <>{this.props.fallback}</>;
      }

      return (
        <div style={{
          padding: '20px',
          maxWidth: '600px',
          margin: '50px auto',
          backgroundColor: '#f8f9fa',
          border: '1px solid #dee2e6',
          borderRadius: '8px',
          textAlign: 'center'
        }}>
          <h2 style={{ color: '#dc3545', marginBottom: '20px' }}>
            Oops! Something went wrong
          </h2>
          
          <p style={{ marginBottom: '20px', color: '#6c757d' }}>
            We're sorry for the inconvenience. The application encountered an unexpected error.
          </p>

          {process.env.NODE_ENV !== 'production' && this.state.error && (
            <details style={{
              textAlign: 'left',
              marginTop: '20px',
              padding: '10px',
              backgroundColor: '#fff',
              border: '1px solid #dee2e6',
              borderRadius: '4px'
            }}>
              <summary style={{
                cursor: 'pointer',
                fontWeight: 'bold',
                marginBottom: '10px'
              }}>
                Error Details (Development Only)
              </summary>
              <pre style={{
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                fontSize: '12px',
                color: '#dc3545'
              }}>
                {this.state.error.toString()}
                {this.state.errorInfo && this.state.errorInfo.componentStack}
              </pre>
            </details>
          )}

          <div style={{ marginTop: '20px' }}>
            <button
              onClick={this.handleReset}
              style={{
                padding: '10px 20px',
                marginRight: '10px',
                backgroundColor: '#007bff',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Try Again
            </button>
            
            <button
              onClick={() => window.location.href = '/'}
              style={{
                padding: '10px 20px',
                backgroundColor: '#6c757d',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              Go to Home
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;