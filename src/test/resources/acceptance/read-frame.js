const area = document.querySelector('[data-testid=account-area]')
return JSON.stringify({
  loggedInAs: area?.querySelector('[data-testid=logged-in-as]')?.textContent ?? null,
  buttons: [...(area?.querySelectorAll('button') ?? [])].map((button) => button.textContent.trim()),
})
