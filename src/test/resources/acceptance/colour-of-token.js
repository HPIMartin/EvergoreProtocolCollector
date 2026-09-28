const [token] = arguments
if (getComputedStyle(document.documentElement).getPropertyValue(token).trim() === '') {
  throw new Error('The page defines no design token ' + token)
}
const probe = document.createElement('span')
probe.style.color = 'var(' + token + ')'
document.body.appendChild(probe)
const colour = getComputedStyle(probe).color
probe.remove()
return colour
