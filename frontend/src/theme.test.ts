import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

const tested = readFileSync(join(import.meta.dirname, 'index.css'), 'utf8')

const tokenBlock = /^:root \{\n([\s\S]*?)\n\}\n/.exec(tested)
const insideTokenBlock = tokenBlock?.[1] ?? ''
const outsideTokenBlock = tested.slice(tokenBlock?.[0].length ?? 0)

const matchesOf = (source: string, pattern: RegExp): string[] =>
  Array.from(source.matchAll(pattern), (match) => match[1])

const declaredTokens = matchesOf(insideTokenBlock, /^ {2}(--[a-z0-9-]+):/gm)
const referencedTokens = matchesOf(tested, /var\((--[a-z0-9-]+)\)/g)

const colourProperties = [
  'color',
  'background-color',
  'background',
  'border-color',
  'border-top-color',
  'border-right-color',
  'border-bottom-color',
  'border-left-color',
  'border-top',
  'border-right',
  'border-bottom',
  'border-left',
  'border',
  'outline-color',
  'outline',
  'box-shadow',
  'fill',
  'stroke',
]

const colourless = /^(?:none|0|inherit|transparent|currentcolor)$/i

const paintedValues = (source: string): string[] =>
  matchesOf(
    source,
    new RegExp(`^ +(?:${colourProperties.join('|')}): ([^;]+);$`, 'gm'),
  )

describe('the theme stylesheet', () => {
  it('groups its tokens into colour, typography and spacing', () => {
    const groups = ['--color-', '--font-', '--space-']

    const missing = groups.filter(
      (group) => !declaredTokens.some((token) => token.startsWith(group)),
    )

    expect(missing).toEqual([])
  })

  it('declares every token it references', () => {
    const undeclared = referencedTokens.filter(
      (token) => !declaredTokens.includes(token),
    )

    expect(undeclared).toEqual([])
  })

  it('references every token it declares', () => {
    const unused = declaredTokens.filter(
      (token) => !referencedTokens.includes(token),
    )

    expect(unused).toEqual([])
  })

  it('declares each token exactly once', () => {
    const duplicates = declaredTokens.filter(
      (token, index) => declaredTokens.indexOf(token) !== index,
    )

    expect(duplicates).toEqual([])
  })

  it('keeps every literal colour inside the token block', () => {
    const literals = outsideTokenBlock.match(
      /#[0-9a-f]{3,8}\b|\b(?:rgba?|hsla?)\(/gi,
    )

    expect(literals).toBeNull()
  })

  it('paints every colour-bearing property from a token', () => {
    const untokenized = paintedValues(outsideTokenBlock).filter(
      (value) => !value.includes('var(--') && !colourless.test(value),
    )

    expect(untokenized).toEqual([])
  })
})
