const [label] = arguments
const stat = [...document.querySelectorAll('[data-testid=stat-header] > div')].find((div) => div.querySelector('dt').textContent.trim() === label)
return stat === undefined ? null : stat.querySelector('dd')
