# Risk Register: {{PROJECT_NAME}}

What can hurt the project or its data, and the countermeasure.

- Per-item detail stays in [backlog.md](backlog.md).
  - An ID here points to a still-live item, after its words (DOC-8).
- A risk mitigated to acceptance, or handled, is removed (DOC-5).
  - Why: git is the history.
- Re-check this file whenever the [roadmap](roadmap.md) changes.
- L/I = likelihood/impact: H/M/L.
- The countermeasure cell holds one fact plus a link to its home (a KB section, a backlog item).
- A risk the project accepts without a countermeasure stays: its countermeasure cell starts with
  "Accepted" and gives the reason.

## Risks by category

> **ADAPT:** one `###` section per category, each with the table below, e.g. data, external
> dependencies, security, process, latent tech debt.

### Category

| Risk | L | I | Countermeasure |
|------|---|---|----------------|

<!-- MODULE agent-seat -->
### Security

| Risk | L | I | Countermeasure |
|------|---|---|----------------|
| An agent pushes what it should not, with the author's rights on a supervised machine or with the agent seat's deploy key | L | H | The seat's only push route is `agent/push`, which never pushes `main` or a tag (the push denies and the guard's `git push` refusal close the other routes, and a spelling the guard cannot parse meets the classifier alone: [working-with-ai-agents.md](knowledge-base/working-with-ai-agents.md), "Permissions & autonomy") |
| The agent seat reaches the GitHub API as an App of its own, so an agent there could try to merge or to push through the API | L | H | The App's narrow rights: it writes only pull requests and issues ([dev-environment.md](knowledge-base/dev-environment.md), "Where agents run") |
| The agent seat runs a Docker daemon of its own, so a container started there reaches what that daemon reaches, and a privileged seat container could take control of its host | L | H | No host socket on the seat, the agent image's blanked `devcontainer.metadata` label, and a user-namespace runtime where the seat machine has one ([dev-environment.md](knowledge-base/dev-environment.md), "Docker on the agent seat") |

<!-- /MODULE agent-seat -->
