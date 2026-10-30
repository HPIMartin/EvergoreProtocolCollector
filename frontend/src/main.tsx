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

const post = (url: string, body: string): Promise<Response> =>
  fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
  })

createRoot(rootElement).render(
  <StrictMode>
    <App get={get} post={post} session={httpSessionApi(get, post)} />
  </StrictMode>,
)
