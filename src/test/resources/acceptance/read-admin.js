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
return JSON.stringify({
  messages: [...document.querySelectorAll('p[data-testid]')].map(textOf),
  accounts: [...document.querySelectorAll('[data-testid=account-row]')].map((row) => ({
    name: cellOf(row, 'account-name'),
    role: roleOf(row),
    roleChoices: roleChoicesOf(row),
    rests: cellOf(row, 'account-rests'),
    lastLogin: cellOf(row, 'account-last-login'),
    issued: cellOf(row, 'account-issued'),
  })),
  question: textOf(document.querySelector('[data-testid=reset-question]')),
})
