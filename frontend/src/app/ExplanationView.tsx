import type { Answer, AnswerGroup } from '../domain'
import { calculationTexts, explanationFrom } from '../domain'

import examples from './explanation/explanation.json'

const EXPLANATION = explanationFrom(examples)

export function ExplanationView() {
  return (
    <section className="explanation">
      <h2 data-testid="view-title">So entstehen eure Zahlen</h2>
      <div className="explanation__legend" data-testid="explanation-legend">
        {EXPLANATION.personas.map((persona) => (
          <p data-testid="legend-entry" key={persona.name}>
            {persona.sentence}
          </p>
        ))}
      </div>
      {EXPLANATION.groups.map((group) => (
        <Group group={group} key={group.title} />
      ))}
    </section>
  )
}

function Group({ group }: { readonly group: AnswerGroup }) {
  return (
    <section className="explanation__group" data-testid="explanation-group">
      <h3 data-testid="group-title">{group.title}</h3>
      {group.answers.map((answer) => (
        <AnswerCard answer={answer} key={answer.question} />
      ))}
    </section>
  )
}

function AnswerCard({ answer }: { readonly answer: Answer }) {
  const calculations = calculationTexts(answer.lines)

  return (
    <article className="explanation__answer" data-testid="answer">
      <h4 data-testid="question">{answer.question}</h4>
      <div data-testid="rule">
        <p>{answer.rule}</p>
        {answer.points.length === 0 ? null : (
          <ul>
            {answer.points.map((point) => (
              <li key={point}>{point}</li>
            ))}
          </ul>
        )}
      </div>
      <div className="explanation__example" data-testid="example">
        <p data-testid="example-intro">{`Beispiel: ${answer.intro}`}</p>
        {calculations.length === 0 ? null : (
          <ul className="explanation__calculations">
            {calculations.map((calculation) => (
              <li data-testid="calculation" key={calculation}>
                {calculation}
              </li>
            ))}
          </ul>
        )}
      </div>
      <p className="explanation__why" data-testid="why">
        {answer.why}
      </p>
    </article>
  )
}
