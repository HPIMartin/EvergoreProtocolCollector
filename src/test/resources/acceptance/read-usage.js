const textOf = (element) => element === null ? null : element.textContent.trim()
const table = document.querySelector('[data-testid=usage]')
const chosen = document.querySelector('[data-testid=switch-usage-window] input:checked')
return JSON.stringify({
  window: chosen === null ? null : textOf(chosen.parentElement),
  headers: table === null ? [] : [...table.querySelectorAll('thead th')].map(textOf),
  rows: table === null ? [] : [...table.querySelectorAll('tbody tr, tfoot tr')].map((row) => ({
    kind: row.getAttribute('data-testid'),
    cells: [...row.querySelectorAll('td')].map(textOf),
  })),
})
