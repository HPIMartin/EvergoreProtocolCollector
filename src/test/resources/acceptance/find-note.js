const [rowWanted, cellWanted] = arguments
const textOf = (element) => {
  const copy = element.cloneNode(true)
  copy.querySelectorAll('[role=note]').forEach((note) => note.remove())
  return copy.textContent.trim()
}
const row = rowWanted.guild
  ? document.querySelector('[data-testid=total-row]')
  : [...document.querySelectorAll('[data-testid=data-row]')].find((candidate) => textOf(candidate.children[0]) === rowWanted.member)
if (row === undefined || row === null) {
  return null
}
return cellWanted.mark
  ? row.querySelector('[data-testid=row-mark], [data-testid=total-mark]')
  : row.children[cellWanted.column].querySelector('[data-testid=cell-mark], [data-testid=total-cell-mark]')
