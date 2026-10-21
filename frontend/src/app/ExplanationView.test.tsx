import { cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import approved from './explanation/explanation.approved.txt?raw'
import { ExplanationView } from './ExplanationView.tsx'

afterEach(cleanup)

function textOf(element: HTMLElement | null): string {
  return element?.textContent ?? ''
}

function answerText(answer: HTMLElement): readonly string[] {
  const rule = within(answer).getByTestId('rule')
  return [
    '',
    `### ${textOf(within(answer).getByTestId('question'))}`,
    textOf(rule.querySelector('p')),
    ...Array.from(rule.querySelectorAll('li'), (point) => `- ${textOf(point)}`),
    textOf(within(answer).getByTestId('example-intro')),
    ...within(answer)
      .queryAllByTestId('calculation')
      .map((calculation) => textOf(calculation)),
    textOf(within(answer).getByTestId('why')),
  ]
}

function pageText(): string {
  const lines = [
    textOf(screen.getByTestId('view-title')),
    '',
    ...screen.getAllByTestId('legend-entry').map((entry) => textOf(entry)),
  ]
  for (const group of screen.getAllByTestId('explanation-group')) {
    lines.push('', `## ${textOf(within(group).getByTestId('group-title'))}`)
    for (const answer of within(group).getAllByTestId('answer')) {
      lines.push(...answerText(answer))
    }
  }
  return `${lines.join('\n')}\n`
}

function inDocumentOrder(approvedText: string): string {
  return approvedText
    .split('\n')
    .map((line) => line.replace(/^(?:#{2,3} |- )/, ''))
    .join('')
}

describe('ExplanationView', () => {
  it('shows exactly the text the author approved', () => {
    render(<ExplanationView />)

    const shown = pageText()

    expect(shown).toBe(approved)
  })

  it('shows nothing besides the approved text, in its order', () => {
    const { container } = render(<ExplanationView />)

    expect(container.textContent).toBe(inDocumentOrder(approved))
  })
})
