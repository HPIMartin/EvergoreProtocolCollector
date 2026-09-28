const textOf = (element) => {
  if (element === null) {
    return null
  }
  const copy = element.cloneNode(true)
  copy.querySelectorAll('[role=note]').forEach((note) => note.remove())
  return copy.textContent.trim()
}
const table = document.querySelector('table.data-table')
const sortOf = () => {
  const sorted = table === null ? null : [...table.querySelectorAll('thead th')].find((th) => th.getAttribute('aria-sort') !== 'none')
  return sorted === null || sorted === undefined
    ? null
    : { column: textOf(sorted.querySelector('[data-testid=column-label]')), direction: sorted.getAttribute('aria-sort') }
}
return JSON.stringify({
  heading: textOf(document.querySelector('[data-testid=view-title]')),
  caption: table === null ? null : textOf(table.querySelector('caption')),
  headers: table === null ? [] : [...table.querySelectorAll('thead [data-testid=column-label]')].map(textOf),
  rows: table === null ? [] : [...table.querySelectorAll('[data-testid=data-row]')].map((row) => [...row.children].map(textOf)),
  emptyMessage: table === null ? null : textOf(table.querySelector('tbody [data-testid=status-panel]')),
  hasPrevious: document.querySelector('[data-testid=pagination-previous]') !== null,
  hasNext: document.querySelector('[data-testid=pagination-next]') !== null,
  sort: sortOf(),
})
