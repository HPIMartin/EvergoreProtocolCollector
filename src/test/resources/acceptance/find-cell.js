const [rowWanted, cellWanted] = arguments
const row = [...document.querySelectorAll('[data-testid=data-row]')].find((candidate) => candidate.children[0].textContent.trim() === rowWanted.member)
if (row === undefined || row === null) {
  return null
}
return row.children[cellWanted.column]
