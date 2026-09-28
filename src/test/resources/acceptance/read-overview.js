const textOf = (element) => {
  if (element === null) {
    return null
  }
  const copy = element.cloneNode(true)
  copy.querySelectorAll('[role=note]').forEach((note) => note.remove())
  return copy.textContent.trim()
}
const noteOf = (element) => {
  if (element === null || element.closest('[aria-hidden=true]') !== null) {
    return null
  }
  const copy = element.cloneNode(true)
  copy.querySelectorAll('[aria-hidden=true]').forEach((hidden) => hidden.remove())
  return copy.textContent.trim()
}
const rowOf = (row, markId, cellMarkId) => ({
  cells: [...row.children].map(textOf),
  mark: noteOf(row.querySelector(`[data-testid=${markId}]`)),
  notes: [...row.children].map((cell) => noteOf(cell.querySelector(`[data-testid=${cellMarkId}]`))),
  hrefs: [...row.children].map((cell) => cell.querySelector('a')?.getAttribute('href') ?? null),
})
const rosterOf = (testId) => {
  const roster = document.querySelector(`[data-testid=${testId}]`)
  if (roster === null) {
    return null
  }
  const total = roster.querySelector('[data-testid=total-row]')
  const sample = total ?? roster.querySelector('[data-testid=data-row]')
  const figureIndex = sample === null ? -1 : [...sample.children].findIndex((cell) => /-figure$/.test(cell.dataset.testid))
  const headers = [...roster.querySelectorAll('thead [data-testid=column-label]')].map(textOf)
  return {
    caption: textOf(roster.querySelector('caption')),
    headers,
    figureHeader: figureIndex < 0 ? null : headers[figureIndex],
    rows: [...roster.querySelectorAll('[data-testid=data-row]')].map((row) => rowOf(row, 'row-mark', 'cell-mark')),
    total: total === null ? null : rowOf(total, 'total-mark', 'total-cell-mark'),
    emptyMessage: textOf(roster.querySelector('tbody [data-testid=status-panel]')),
  }
}
return JSON.stringify({
  active: rosterOf('active-roster'),
  dormant: rosterOf('dormant-roster'),
  position: [...document.querySelectorAll('[data-testid=stat-header] > div')].map((stat) => ({
    label: textOf(stat.querySelector('dt')),
    value: textOf(stat.querySelector('dd')),
  })),
  messages: [...document.querySelectorAll('[data-testid=status-panel]')].map(textOf),
})
