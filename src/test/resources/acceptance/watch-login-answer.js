if (!window.epcFetchWatched) {
  window.epcFetchWatched = true
  const original = window.fetch
  window.fetch = (...request) =>
    original(...request).then((response) => {
      window.epcLoginAnswer = response.status
      return response
    })
}
window.epcLoginAnswer = null
