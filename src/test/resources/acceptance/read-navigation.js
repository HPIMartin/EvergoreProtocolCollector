return JSON.stringify({
  frame: [...document.querySelectorAll('nav[aria-label="Hauptnavigation"] a')].map((link) => ({
    label: link.textContent.trim(),
    href: link.getAttribute('href'),
    current: link.getAttribute('aria-current') === 'page',
  })),
  allHrefs: [...document.querySelectorAll('a[href]')].map((link) => link.getAttribute('href')),
})
