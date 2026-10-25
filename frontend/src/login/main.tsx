import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../index.css'
import { LoginPage } from './LoginPage.tsx'

const rootElement = document.getElementById('root')
if (!rootElement) {
  throw new Error('Root element not found')
}

createRoot(rootElement).render(
  <StrictMode>
    <LoginPage
      search={window.location.search}
      post={(url, body) =>
        fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body,
        })
      }
      navigate={(target) => window.location.assign(target)}
    />
  </StrictMode>,
)
