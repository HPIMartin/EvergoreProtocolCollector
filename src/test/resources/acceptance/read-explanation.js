const textOf = (element) => element === null ? null : element.textContent.trim()
const within = (element, testId) => element.querySelector(`[data-testid=${testId}]`)
const levelOf = (element) => element !== null && /^H[1-6]$/.test(element.tagName) ? Number(element.tagName[1]) : 0
const answerOf = (answer) => ({
  question: textOf(within(answer, 'question')),
  questionLevel: levelOf(within(answer, 'question')),
  example: textOf(within(answer, 'example')),
  examples: answer.querySelectorAll('[data-testid=example]').length,
  ruleBeforeExample: (() => {
    const rule = within(answer, 'rule')
    const example = within(answer, 'example')
    return rule !== null && example !== null && !rule.contains(example) && !example.contains(rule) && (rule.compareDocumentPosition(example) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0
  })(),
})
const content = document.querySelector('[data-testid=page-content]')
return JSON.stringify({
  text: content === null ? '' : content.innerText,
  legend: [...document.querySelectorAll('[data-testid=legend-entry]')].map(textOf),
  legendAboveTheGroups: (() => {
    const legend = document.querySelector('[data-testid=explanation-legend]')
    const group = document.querySelector('[data-testid=explanation-group]')
    return legend !== null && group !== null && (legend.compareDocumentPosition(group) & Node.DOCUMENT_POSITION_FOLLOWING) !== 0
  })(),
  groups: [...document.querySelectorAll('[data-testid=explanation-group]')].map((group) => ({
    title: textOf(within(group, 'group-title')),
    titleLevel: levelOf(within(group, 'group-title')),
    answers: [...group.querySelectorAll('[data-testid=answer]')].map(answerOf),
  })),
})
