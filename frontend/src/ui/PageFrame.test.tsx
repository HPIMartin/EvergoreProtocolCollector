import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import type { PageFrameAccount } from './PageFrame.tsx'
import { PageFrame } from './PageFrame.tsx'

const navigation = [
  { label: 'Übersicht', href: '/overview', current: false },
  { label: 'Bank', href: '/avatars/Bambor/bank', current: true },
  { label: 'Lager', href: '/avatars/Bambor/storage', current: false },
]

const renderFrame = (onFollow?: (href: string) => void) =>
  render(
    <PageFrame brand="Gildenbank" navigation={navigation} onFollow={onFollow}>
      <p>Der Inhalt der Seite</p>
    </PageFrame>,
  )

describe('PageFrame', () => {
  afterEach(cleanup)

  it('renders the brand it is given', () => {
    renderFrame()

    expect(screen.getByTestId('page-brand').textContent).toBe('Gildenbank')
  })

  it('renders one link per navigation entry', () => {
    renderFrame()

    const labels = screen.getAllByRole('link').map((link) => link.textContent)

    expect(labels).toEqual(['Übersicht', 'Bank', 'Lager'])
  })

  it('points each link at the target of its entry', () => {
    renderFrame()

    const targets = screen
      .getAllByRole('link')
      .map((link) => link.getAttribute('href'))

    expect(targets).toEqual([
      '/overview',
      '/avatars/Bambor/bank',
      '/avatars/Bambor/storage',
    ])
  })

  it('marks only the current entry as the current page', () => {
    renderFrame()

    const marks = screen
      .getAllByRole('link')
      .map((link) => link.getAttribute('aria-current'))

    expect(marks).toEqual([null, 'page', null])
  })

  it('names its navigation landmark', () => {
    renderFrame()

    const label = screen.getByRole('navigation').getAttribute('aria-label')

    expect(label).toBe('Hauptnavigation')
  })

  it('renders its children into the content region', () => {
    renderFrame()

    expect(screen.getByTestId('page-content').textContent).toBe(
      'Der Inhalt der Seite',
    )
  })
})

interface Clicked {
  readonly loggedOut: number[]
  readonly passwordChanges: number[]
}

function renderFrameOf(account: Partial<PageFrameAccount>): Clicked {
  const clicked: Clicked = { loggedOut: [], passwordChanges: [] }
  render(
    <PageFrame
      brand="Gildenbank"
      navigation={navigation}
      account={{
        name: 'Aurora',
        canChangePassword: true,
        logOutFailed: false,
        onLogOut: () => clicked.loggedOut.push(1),
        onChangePassword: () => clicked.passwordChanges.push(1),
        ...account,
      }}
    >
      <p>Der Inhalt der Seite</p>
    </PageFrame>,
  )

  return clicked
}

describe('PageFrame account area', () => {
  afterEach(cleanup)

  it('shows no account area without an account', () => {
    renderFrame()

    expect(screen.queryByTestId('account-area')).toBeNull()
  })

  it('says who is logged in', () => {
    renderFrameOf({ name: 'Ärger' })

    expect(screen.getByTestId('logged-in-as').textContent).toBe(
      'Angemeldet als Ärger',
    )
  })

  it('offers the buttons to log out and to change the password', () => {
    renderFrameOf({})

    const buttons = screen
      .getAllByRole('button')
      .map((button) => button.textContent)

    expect(buttons).toEqual(['Abmelden', 'Passwort ändern'])
  })

  it('offers no change of a password to a login that has none', () => {
    renderFrameOf({ canChangePassword: false })

    const buttons = screen
      .getAllByRole('button')
      .map((button) => button.textContent)

    expect(buttons).toEqual(['Abmelden'])
  })

  it.each([
    [true, ['Die Abmeldung hat nicht geklappt. Bitte erneut versuchen.']],
    [false, []],
  ])(
    'shows the failure of a logout in the account area only when the logout failed: %s',
    (logOutFailed, shown) => {
      renderFrameOf({ logOutFailed })

      const texts = within(screen.getByTestId('account-area'))
        .queryAllByTestId('status-panel')
        .map((panel) => panel.textContent)

      expect(texts).toStrictEqual(shown)
    },
  )

  it('keeps the account area out of the navigation links', () => {
    renderFrameOf({})

    const labels = screen.getAllByRole('link').map((link) => link.textContent)

    expect(labels).toEqual(['Übersicht', 'Bank', 'Lager'])
  })

  it('reports a click on Abmelden', () => {
    const clicked = renderFrameOf({})

    fireEvent.click(screen.getByRole('button', { name: 'Abmelden' }))

    expect(clicked).toStrictEqual({ loggedOut: [1], passwordChanges: [] })
  })

  it('reports a click on Passwort ändern', () => {
    const clicked = renderFrameOf({})

    fireEvent.click(screen.getByRole('button', { name: 'Passwort ändern' }))

    expect(clicked).toStrictEqual({ loggedOut: [], passwordChanges: [1] })
  })
})

describe('PageFrame navigation', () => {
  afterEach(cleanup)

  it('reports a plain click instead of letting the browser reload the shell', () => {
    const followed: string[] = []
    renderFrame((href) => {
      followed.push(href)
    })

    const notCancelled = fireEvent.click(
      screen.getByRole('link', { name: 'Lager' }),
    )

    expect({ notCancelled, followed }).toStrictEqual({
      notCancelled: false,
      followed: ['/avatars/Bambor/storage'],
    })
  })

  it('leaves the click to the browser when nobody listens', () => {
    renderFrame()

    const notCancelled = fireEvent.click(
      screen.getByRole('link', { name: 'Lager' }),
    )

    expect(notCancelled).toBe(true)
  })
})
