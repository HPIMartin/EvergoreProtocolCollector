const textOf = (element) => element === null ? null : element.textContent.trim()
const cellOf = (row, testId) => textOf(row.querySelector('[data-testid=' + testId + ']'))
return JSON.stringify({
  messages: [...document.querySelectorAll('p[data-testid]')].map(textOf),
  accounts: [...document.querySelectorAll('[data-testid=account-row]')].map((row) => ({
    name: cellOf(row, 'account-name'),
    rests: cellOf(row, 'account-rests'),
    lastLogin: cellOf(row, 'account-last-login'),
    issued: cellOf(row, 'account-issued'),
  })),
  question: textOf(document.querySelector('[data-testid=reset-question]')),
})
