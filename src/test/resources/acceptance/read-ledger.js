const textOf = (element) => (element === null ? null : element.textContent.trim())
const table = document.querySelector('[data-testid=page-content] table')
return JSON.stringify({
  caption: table === null ? null : textOf(table.querySelector('caption')),
  headers: table === null ? [] : [...table.querySelectorAll('thead [data-testid=column-label]')].map(textOf),
  rows: table === null ? [] : [...table.querySelectorAll('[data-testid=data-row]')].map((row) => [...row.children].map(textOf)),
  emptyMessage: table === null ? null : textOf(table.querySelector('tbody [data-testid=status-panel]')),
})
