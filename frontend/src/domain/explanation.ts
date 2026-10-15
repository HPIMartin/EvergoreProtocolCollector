type Movement = 'Einlagerung' | 'Entnahme'

type Operator = '×' | '+' | '−'

interface MovementLine {
  readonly kind: 'movement'
  readonly member: string
  readonly movement: Movement
  readonly quantity: number
  readonly item: string
  readonly quality: number
  readonly marketStall: boolean
  readonly atGuildPrice: boolean
  readonly price: number
  readonly share: number
  readonly result: number
}

type Term =
  | { readonly amount: number }
  | { readonly share: number }
  | { readonly line: number }

interface FigureLine {
  readonly kind: 'figure'
  readonly row: string | null
  readonly label: string
  readonly terms: readonly Term[]
  readonly operators: readonly Operator[]
  readonly result: number
}

export type CalculationLine = MovementLine | FigureLine

const FULL_QUALITY = 100

const goldFormat = new Intl.NumberFormat('de-DE', {
  maximumFractionDigits: 2,
})

export const calculationTexts = (lines: readonly CalculationLine[]): string[] =>
  lines.map((line) =>
    line.kind === 'movement' ? movementText(line) : figureText(line, lines),
  )

function movementText(line: MovementLine): string {
  const terms = [
    String(line.quantity),
    gold(line.price),
    share(line.share),
    ...(line.quality === FULL_QUALITY ? [] : [share(line.quality)]),
  ]
  return `${movementOf(line)}: ${terms.join(' × ')} = ${resultOf(line.result)}`
}

function movementOf(line: MovementLine): string {
  const goods = [
    String(line.quantity),
    line.item,
    ...(line.quality === FULL_QUALITY
      ? []
      : [`der Qualität ${String(line.quality)}`]),
    ...(line.marketStall ? ['vom Marktstand'] : []),
  ].join(' ')
  const moved =
    line.movement === 'Einlagerung'
      ? `${line.member} legt ${goods} ein`
      : `${line.member} nimmt ${goods} heraus`
  return line.atGuildPrice ? `${moved}, zum Gildenpreis` : moved
}

function figureText(
  line: FigureLine,
  lines: readonly CalculationLine[],
): string {
  const opener = line.row === null ? line.label : `${line.row} ${line.label}`
  const terms = line.terms.map((term) => termText(term, lines))
  const expression = terms.reduce(
    (text, term, index) => `${text} ${line.operators[index - 1] ?? ''} ${term}`,
  )
  return `${opener}: ${expression} = ${resultOf(line.result)}`
}

function termText(term: Term, lines: readonly CalculationLine[]): string {
  if ('share' in term) {
    return share(term.share)
  }
  const value =
    'line' in term ? (lines[term.line]?.result ?? Number.NaN) : term.amount
  return value < 0 ? `(${gold(value)})` : gold(value)
}

function resultOf(exact: number): string {
  const shown = wholeGoldOf(exact)
  return shown === exact ? gold(exact) : `${gold(exact)} → ${gold(shown)}`
}

function wholeGoldOf(exact: number): number {
  return exact < 0 ? -Math.round(-exact) : Math.round(exact)
}

function gold(value: number): string {
  return `${goldFormat.format(value === 0 ? 0 : value)} Gold`
}

function share(percent: number): string {
  return `${String(percent)} %`
}

export interface Persona {
  readonly name: string
  readonly sentence: string
}

export interface Answer {
  readonly question: string
  readonly rule: string
  readonly points: readonly string[]
  readonly intro: string
  readonly lines: readonly CalculationLine[]
  readonly why: string
}

export interface AnswerGroup {
  readonly title: string
  readonly answers: readonly Answer[]
}

export interface Explanation {
  readonly personas: readonly Persona[]
  readonly groups: readonly AnswerGroup[]
}

type Fields = { readonly [key: string]: unknown }

export function explanationFrom(raw: unknown): Explanation {
  const data = recordOf(raw, 'the explanation')
  return {
    personas: listOf(data.personas, 'its personas').map((persona) => {
      const fields = recordOf(persona, 'a persona')
      return {
        name: textOf(fields.name, "a persona's name"),
        sentence: textOf(fields.sentence, "a persona's sentence"),
      }
    }),
    groups: listOf(data.groups, 'its groups').map(groupOf),
  }
}

function groupOf(raw: unknown): AnswerGroup {
  const group = recordOf(raw, 'a group')
  return {
    title: textOf(group.title, "a group's title"),
    answers: listOf(group.answers, "a group's answers").map(answerOf),
  }
}

function answerOf(raw: unknown): Answer {
  const answer = recordOf(raw, 'an answer')
  const example = recordOf(answer.example, 'an example')
  return {
    question: textOf(answer.question, 'a question'),
    rule: textOf(answer.rule, 'a rule'),
    points:
      answer.points === undefined
        ? []
        : listOf(answer.points, "a rule's points").map((point) =>
            textOf(point, 'a point'),
          ),
    intro: textOf(example.intro, "an example's intro"),
    lines: listOf(example.lines, "an example's lines").map((line, index) =>
      lineOf(line, index),
    ),
    why: textOf(answer.why, 'a sentence on why'),
  }
}

function lineOf(raw: unknown, index: number): CalculationLine {
  const line = recordOf(raw, 'a calculation')
  if (line.kind === 'movement') {
    return {
      kind: 'movement',
      member: textOf(line.member, "a movement's member"),
      movement: movementFrom(line.movement),
      quantity: numberOf(line.quantity, 'a quantity'),
      item: textOf(line.item, 'an item'),
      quality: numberOf(line.quality, 'a quality'),
      marketStall: flagOf(line.marketStall, 'the market stall'),
      atGuildPrice: flagOf(line.atGuildPrice, 'the guild price'),
      price: numberOf(line.price, 'a price'),
      share: numberOf(line.share, 'a share'),
      result: numberOf(line.result, 'a result'),
    }
  }
  if (line.kind === 'figure') {
    const terms = listOf(line.terms, "a figure's terms").map((term) =>
      termOf(term, index),
    )
    const operators = listOf(line.operators, "a figure's operators").map(
      operatorOf,
    )
    if (operators.length !== terms.length - 1) {
      throw unreadable("a figure's operators")
    }
    return {
      kind: 'figure',
      row: line.row === null ? null : textOf(line.row, "a figure's row"),
      label: textOf(line.label, "a figure's label"),
      terms,
      operators,
      result: numberOf(line.result, 'a result'),
    }
  }
  throw unreadable('a calculation of an unknown kind')
}

function termOf(raw: unknown, before: number): Term {
  const term = recordOf(raw, 'a term')
  if (term.line !== undefined && term.line !== null) {
    const line = numberOf(term.line, "a term's line")
    if (!Number.isInteger(line) || line < 0 || line >= before) {
      throw unreadable("a term's line")
    }
    return { line }
  }
  if (term.share !== undefined && term.share !== null) {
    return { share: numberOf(term.share, "a term's share") }
  }
  return { amount: numberOf(term.amount, "a term's amount") }
}

function movementFrom(raw: unknown): Movement {
  if (raw === 'Einlagerung' || raw === 'Entnahme') {
    return raw
  }
  throw unreadable('a movement')
}

function operatorOf(raw: unknown): Operator {
  if (raw === '×' || raw === '+' || raw === '−') {
    return raw
  }
  throw unreadable('an operator')
}

function recordOf(raw: unknown, what: string): Fields {
  if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) {
    throw unreadable(what)
  }
  return raw as Fields
}

function listOf(raw: unknown, what: string): readonly unknown[] {
  if (!Array.isArray(raw)) {
    throw unreadable(what)
  }
  return raw
}

function textOf(raw: unknown, what: string): string {
  if (typeof raw !== 'string') {
    throw unreadable(what)
  }
  return raw
}

function numberOf(raw: unknown, what: string): number {
  if (typeof raw !== 'number' || !Number.isFinite(raw)) {
    throw unreadable(what)
  }
  return raw
}

function flagOf(raw: unknown, what: string): boolean {
  if (typeof raw !== 'boolean') {
    throw unreadable(what)
  }
  return raw
}

function unreadable(what: string): TypeError {
  return new TypeError(`The explanation page cannot read ${what}.`)
}
