import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { httpSessionApi } from './api'
import { App } from './app/App.tsx'

const rootElement = document.getElementById('root')
if (!rootElement) {
  throw new Error('Root element not found')
}

const get = (url: string): Promise<Response> => fetch(url)

createRoot(rootElement).render(
  <StrictMode>
    <App
      get={get}
      session={httpSessionApi(get, (url, body) =>
        fetch(url, { method: 'POST', body }),
      )}
    />
  </StrictMode>,
)
