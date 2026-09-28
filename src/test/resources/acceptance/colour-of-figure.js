const [holder] = arguments
const walker = document.createTreeWalker(holder, NodeFilter.SHOW_TEXT, {
  acceptNode: (node) => (node.textContent.trim() === '' ? NodeFilter.FILTER_SKIP : NodeFilter.FILTER_ACCEPT),
})
let figure = null
for (let node = walker.nextNode(); node !== null; node = walker.nextNode()) {
  figure = node
}
if (figure === null) {
  throw new Error('The figure holds no text')
}
const style = getComputedStyle(figure.parentElement)
return style.webkitTextFillColor || style.color
