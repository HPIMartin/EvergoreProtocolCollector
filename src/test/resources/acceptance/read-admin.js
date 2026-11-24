const textOf = (element) => element === null ? null : element.textContent.trim()
const cellOf = (row, testId) => textOf(row.querySelector('[data-testid=' + testId + ']'))
const roleFieldOf = (row) => row.querySelector('[data-testid=account-role]')
const isChoice = (field) => field !== null && field.tagName === 'SELECT'
const roleOf = (row) => {
  const field = roleFieldOf(row)
  return isChoice(field) ? textOf(field.selectedOptions[0]) : textOf(field)
}
const roleChoicesOf = (row) => {
  const field = roleFieldOf(row)
  return isChoice(field) ? [...field.options].map(textOf) : []
}
const linkOf = (anchor) => ({ text: textOf(anchor), href: anchor.getAttribute('href') })
const linksIn = (element) => [...element.querySelectorAll('a')].map(linkOf)
const findingListOf = (paragraph) => {
  const text = textOf(paragraph)
  return {
    heading: text.split(': ')[0],
    text,
    entries: [...paragraph.querySelectorAll('[data-testid=finding]')].map((entry) => ({ text: textOf(entry), links: linksIn(entry) })),
    links: linksIn(paragraph),
  }
}
const paragraphs = [...document.querySelectorAll('p[data-testid]')]
return JSON.stringify({
  messages: paragraphs.map(textOf),
  findings: paragraphs.map(findingListOf),
  accounts: [...document.querySelectorAll('[data-testid=account-row]')].map((row) => ({
    name: cellOf(row, 'account-name'),
    role: roleOf(row),
    roleChoices: roleChoicesOf(row),
    rests: cellOf(row, 'account-rests'),
    lastLogin: cellOf(row, 'account-last-login'),
    issued: cellOf(row, 'account-issued'),
  })),
  question: textOf(document.querySelector('[data-testid=reset-question]')),
  startButton: textOf(document.querySelector('[data-testid=start-collection]')),
})
