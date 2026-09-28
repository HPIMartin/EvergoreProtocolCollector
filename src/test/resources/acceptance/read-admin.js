const textOf = (element) => element === null ? null : element.textContent.trim()
return JSON.stringify({
  messages: [...document.querySelectorAll('p[data-testid]')].map(textOf),
})
