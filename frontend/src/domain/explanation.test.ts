import { describe, expect, it } from 'vitest'

import type { CalculationLine } from './explanation.ts'
import { calculationTexts, explanationFrom } from './explanation.ts'

const deposit = (
  overrides: Partial<Extract<CalculationLine, { kind: 'movement' }>>,
): CalculationLine => ({
  kind: 'movement',
  member: 'Mira Mitglied',
  movement: 'Einlagerung',
  quantity: 10,
  item: 'Eisenerz',
  quality: 100,
  marketStall: false,
  atGuildPrice: false,
  price: 40,
  share: 0,
  result: 0,
  ...overrides,
})

describe('calculationTexts', () => {
  it('writes a deposit as the member putting the goods in, with its price and share', () => {
    const texts = calculationTexts([deposit({})])

    expect(texts).toStrictEqual([
      'Mira Mitglied legt 10 Eisenerz ein: 10 × 40 Gold × 0 % = 0 Gold',
    ])
  })

  it('names the quality, the market stall and the guild price where they apply', () => {
    const texts = calculationTexts([
      deposit({
        item: 'Kurzbogen',
        quantity: 1,
        quality: 50,
        price: 2000,
        share: 60,
        result: 600,
      }),
      deposit({
        item: 'Steinkohle',
        marketStall: true,
        atGuildPrice: true,
        price: 100,
        share: 60,
        result: 600,
      }),
    ])

    expect(texts).toStrictEqual([
      'Mira Mitglied legt 1 Kurzbogen der Qualität 50 ein: 1 × 2.000 Gold × 60 % × 50 % = 600 Gold',
      'Mira Mitglied legt 10 Steinkohle vom Marktstand ein, zum Gildenpreis: 10 × 100 Gold × 60 % = 600 Gold',
    ])
  })

  it('writes a withdrawal as the member taking the goods out', () => {
    const texts = calculationTexts([
      deposit({
        movement: 'Entnahme',
        item: 'Kurzschwert',
        quantity: 1,
        price: 1200,
        share: 60,
        result: 720,
      }),
    ])

    expect(texts).toStrictEqual([
      'Mira Mitglied nimmt 1 Kurzschwert heraus: 1 × 1.200 Gold × 60 % = 720 Gold',
    ])
  })

  it('shows a result that is no whole gold beside the whole gold the overview shows', () => {
    const texts = calculationTexts([
      deposit({
        item: 'Magische Ätherbinde',
        quantity: 1,
        price: 257,
        share: 60,
        result: 154.2,
      }),
    ])

    expect(texts).toStrictEqual([
      'Mira Mitglied legt 1 Magische Ätherbinde ein: 1 × 257 Gold × 60 % = 154,2 Gold → 154 Gold',
    ])
  })

  it('writes a figure after its row, from amounts, shares and earlier results, a result below zero in brackets as a term', () => {
    const texts = calculationTexts([
      {
        kind: 'figure',
        row: 'Hanna Handwerk',
        label: 'Nach Abzügen',
        terms: [{ amount: 0 }, { amount: 300 }, { amount: 300 }],
        operators: ['−', '−'],
        result: -600,
      },
      {
        kind: 'figure',
        row: 'Mira Mitglied',
        label: 'Bank-Einzahlung',
        terms: [{ amount: 1000 }, { share: 100 }],
        operators: ['×'],
        result: 1000,
      },
      {
        kind: 'figure',
        row: 'Gilde',
        label: 'Nach Abzügen',
        terms: [{ line: 1 }, { line: 0 }],
        operators: ['+'],
        result: 400,
      },
      {
        kind: 'figure',
        row: null,
        label: 'Gildenbank',
        terms: [{ amount: 1000 }, { amount: 300 }],
        operators: ['−'],
        result: 700,
      },
    ])

    expect(texts).toStrictEqual([
      'Hanna Handwerk Nach Abzügen: 0 Gold − 300 Gold − 300 Gold = -600 Gold',
      'Mira Mitglied Bank-Einzahlung: 1.000 Gold × 100 % = 1.000 Gold',
      'Gilde Nach Abzügen: 1.000 Gold + (-600 Gold) = 400 Gold',
      'Gildenbank: 1.000 Gold − 300 Gold = 700 Gold',
    ])
  })

  it('rounds half a gold away from zero, as the overview does', () => {
    const texts = calculationTexts([
      {
        kind: 'figure',
        row: 'Mira Mitglied',
        label: 'Nach Abzügen',
        terms: [{ amount: 0 }, { amount: 0.5 }],
        operators: ['−'],
        result: -0.5,
      },
    ])

    expect(texts).toStrictEqual([
      'Mira Mitglied Nach Abzügen: 0 Gold − 0,5 Gold = -0,5 Gold → -1 Gold',
    ])
  })

  it('refuses a calculation of a kind it cannot write', () => {
    const data = {
      personas: [],
      groups: [
        {
          title: 'Was zählt',
          answers: [
            {
              question: 'Was zählt Gold in der Gildenbank?',
              rule: 'Gold zählt, wie es ist.',
              example: {
                intro: 'Mira Mitglied zahlt ein.',
                lines: [{ kind: 'guess' }],
              },
              why: 'Warum? Gold ist Gold.',
            },
          ],
        },
      ],
    }

    const reading = () => explanationFrom(data)

    expect(reading).toThrow(
      'The explanation page cannot read a calculation of an unknown kind.',
    )
  })
})
